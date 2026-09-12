package com.runningolle.domain.home.service;

import com.runningolle.domain.course.entity.CourseTheme;
import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.course.repository.CourseThemeRepository;
import com.runningolle.domain.home.config.HomeRecommendationProperties;
import com.runningolle.domain.home.dto.RecommendedCoursesResponse;
import com.runningolle.domain.home.repository.CourseRecommendationQueryRepository;
import com.runningolle.domain.home.repository.CourseRecommendationQueryRepository.CourseRecommendationCandidate;
import com.runningolle.domain.home.service.CourseRecommendationContextRetriever.RetrievedRecommendationContext;
import com.runningolle.domain.home.service.CourseRecommendationReranker.BaseRecommendationCandidate;
import com.runningolle.domain.home.service.CourseRecommendationReranker.RecommendationUserPreference;
import com.runningolle.domain.home.service.CourseRecommendationReranker.RerankedRecommendation;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.enums.AccountStatus;
import com.runningolle.domain.user.enums.PreferredDifficulty;
import com.runningolle.domain.user.enums.PreferredDistance;
import com.runningolle.domain.user.enums.UserTypeCode;
import com.runningolle.domain.user.repository.UserRepository;
import com.runningolle.domain.user.repository.UserThemeRepository;
import com.runningolle.domain.user.repository.UserUserTypeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseRecommendationService {

    private static final double DISTANCE_PREFERENCE_WEIGHT = 25.0;
    private static final double DISTANCE_PREFERENCE_SECONDARY_SCORE = 18.0;
    private static final double DISTANCE_PREFERENCE_NEAR_MATCH_SCORE = 15.0;
    private static final double DISTANCE_PREFERENCE_LOW_MATCH_SCORE = 10.0;
    private static final double DISTANCE_PREFERENCE_MINIMAL_MATCH_SCORE = 8.0;
    private static final double DISTANCE_PREFERENCE_FAR_MATCH_SCORE = 7.0;
    private static final double THEME_WEIGHT = 25.0;
    private static final double DIFFICULTY_WEIGHT = 20.0;
    private static final double DIFFICULTY_NEAR_MATCH_SCORE = 10.0;
    private static final double LOCATION_WEIGHT = 15.0;
    private static final double LOCATION_NEARBY_SCORE = 10.0;
    private static final double LOCATION_ACCESSIBLE_SCORE = 6.0;
    private static final double LOCATION_FAR_SCORE = 2.0;
    private static final double USER_TYPE_WEIGHT = 10.0;
    private static final double POPULARITY_WEIGHT = 5.0;
    private static final double HIGH_RATING_SCORE = 3.0;
    private static final double MID_RATING_SCORE = 2.0;
    private static final double LOW_RATING_SCORE = 1.0;
    private static final double HIGH_COMPLETION_SCORE = 2.0;
    private static final double MID_COMPLETION_SCORE = 1.0;
    private static final int RESPONSE_RECOMMENDATION_LIMIT = 3;
    private static final Set<String> RELAXED_TRAVELER_THEME_CODES = Set.of("COAST", "PHOTO", "FOOD", "TRADITION");
    private static final Set<String> REVIEW_SOURCE_TYPES = Set.of("COURSE_REVIEW");
    private static final Map<String, String> THEME_LABELS = Map.of(
            "COAST", "해안 풍경",
            "FOREST", "숲길 분위기",
            "OREUM", "오름 코스",
            "FOOD", "미식 동선",
            "PHOTO", "포토 포인트",
            "TRADITION", "제주 전통 분위기",
            "URBAN", "도심 접근성"
    );

    private final UserRepository userRepository;
    private final UserUserTypeRepository userUserTypeRepository;
    private final UserThemeRepository userThemeRepository;
    private final CourseThemeRepository courseThemeRepository;
    private final CourseRecommendationQueryRepository courseRecommendationQueryRepository;
    private final HomeRecommendationProperties homeRecommendationProperties;
    private final ObjectProvider<CourseRecommendationReranker> courseRecommendationRerankerProvider;
    private final ObjectProvider<CourseRecommendationContextRetriever> courseRecommendationContextRetrieverProvider;
    private final CourseRecommendationRerankValidator courseRecommendationRerankValidator;

    @Transactional(readOnly = true)
    public RecommendedCoursesResponse getRecommendedCourses(UUID userId, Double latitude, Double longitude) {
        validateLocation(latitude, longitude);

        UserPreference preference = loadUserPreference(userId);
        List<ScoredRecommendation> baseScoredRecommendations = scoreBaseRecommendations(preference, latitude, longitude);
        if (baseScoredRecommendations.isEmpty()) {
            return new RecommendedCoursesResponse(List.of());
        }

        List<ScoredRecommendation> topBaseCandidatesForReranking =
                selectTopBaseCandidatesForReranking(baseScoredRecommendations);
        Map<UUID, String> fallbackReasons = fallbackReasonsByCourseId(preference, topBaseCandidatesForReranking);
        List<ScoredRecommendation> finalRecommendations =
                applyOptionalReranking(preference, topBaseCandidatesForReranking);

        return new RecommendedCoursesResponse(toResponseItems(finalRecommendations, fallbackReasons));
    }

    private UserPreference loadUserPreference(UUID userId) {
        User user = userRepository.findById(userId)
                .filter(found -> found.getAccountStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        return new UserPreference(
                userId,
                user.getPreferredDistance(),
                user.getPreferredDifficulty(),
                userUserTypeRepository.findAllByUserId(userId).stream()
                        .map(mapping -> UserTypeCode.valueOf(mapping.getUserType().getCode()))
                        .collect(java.util.stream.Collectors.toSet()),
                userThemeRepository.findAllByUserId(userId).stream()
                        .map(userTheme -> userTheme.getTheme().getCode())
                        .collect(java.util.stream.Collectors.toSet())
        );
    }

    private List<ScoredRecommendation> scoreBaseRecommendations(
            UserPreference preference,
            Double latitude,
            Double longitude
    ) {
        List<CourseRecommendationCandidate> candidates =
                courseRecommendationQueryRepository.findRecommendationCandidates(latitude, longitude);
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<String>> themeCodesByCourseId = themeCodesByCourseId(candidates);
        List<ScoredRecommendation> scoredRecommendations = new ArrayList<>(candidates.size());
        for (CourseRecommendationCandidate candidate : candidates) {
            scoredRecommendations.add(scoreBaseRecommendation(preference, candidate, themeCodesByCourseId));
        }
        return scoredRecommendations;
    }

    private ScoredRecommendation scoreBaseRecommendation(
            UserPreference preference,
            CourseRecommendationCandidate candidate,
            Map<UUID, List<String>> themeCodesByCourseId
    ) {
        List<String> themeCodes = themeCodesByCourseId.getOrDefault(candidate.courseId(), List.of());
        BaseScoreComponents baseScoreComponents = calculateBaseScoreComponents(preference, candidate, themeCodes);
        double baseScore = calculateBaseScore(baseScoreComponents);
        Double distanceFromUserKm = candidate.distanceMeters() == null
                ? null
                : round(candidate.distanceMeters() / 1000.0);
        return new ScoredRecommendation(
                candidate,
                themeCodes,
                preference.userTypes(),
                distanceFromUserKm,
                baseScoreComponents,
                baseScore,
                null,
                baseScore,
                null
        );
    }

    private List<ScoredRecommendation> selectTopBaseCandidatesForReranking(List<ScoredRecommendation> scoredRecommendations) {
        return scoredRecommendations.stream()
                .sorted(baseRecommendationComparator())
                .limit(homeRecommendationProperties.getRerankCandidateLimit())
                .toList();
    }

    private List<RecommendedCoursesResponse.RecommendedCourseItem> toResponseItems(
            List<ScoredRecommendation> finalRecommendations,
            Map<UUID, String> fallbackReasons
    ) {
        return finalRecommendations.stream()
                .sorted(finalRecommendationComparator())
                .limit(RESPONSE_RECOMMENDATION_LIMIT)
                .map(recommendation -> toResponseItem(recommendation, fallbackReasons.get(recommendation.candidate().courseId())))
                .toList();
    }

    private Map<UUID, List<String>> themeCodesByCourseId(List<CourseRecommendationCandidate> candidates) {
        List<UUID> courseIds = candidates.stream().map(CourseRecommendationCandidate::courseId).toList();
        Map<UUID, List<String>> themeCodesByCourseId = new LinkedHashMap<>();
        for (CourseTheme courseTheme : courseThemeRepository.findAllByCourse_IdIn(courseIds)) {
            themeCodesByCourseId.computeIfAbsent(courseTheme.getCourse().getId(), ignored -> new ArrayList<>())
                    .add(courseTheme.getTheme().getCode());
        }
        return themeCodesByCourseId;
    }

    private BaseScoreComponents calculateBaseScoreComponents(
            UserPreference preference,
            CourseRecommendationCandidate candidate,
            List<String> themeCodes
    ) {
        return new BaseScoreComponents(
                calculateDistancePreferenceScore(preference.preferredDistance(), candidate.distanceKm()),
                calculateThemeScore(preference.themeCodes(), themeCodes),
                calculateDifficultyScore(preference.preferredDifficulty(), candidate.difficulty()),
                calculateLocationScore(candidate.distanceMeters()),
                calculateUserTypeScore(preference.userTypes(), candidate, themeCodes),
                calculatePopularityScore(candidate.averageRating(), candidate.completionCount())
        );
    }

    private List<ScoredRecommendation> applyOptionalReranking(
            UserPreference preference,
            List<ScoredRecommendation> topBaseCandidates
    ) {
        if (!homeRecommendationProperties.isAiRerankingEnabled()) {
            return topBaseCandidates;
        }

        CourseRecommendationReranker reranker = courseRecommendationRerankerProvider.getIfAvailable();
        if (reranker == null) {
            log.info("Home recommendation AI reranker is enabled but no reranker bean is registered.");
            return topBaseCandidates;
        }

        try {
            List<BaseRecommendationCandidate> baseCandidates = topBaseCandidates.stream()
                    .map(ScoredRecommendation::toBaseRecommendationCandidate)
                    .toList();
            Map<UUID, RerankedRecommendation> rerankedByCourseId = courseRecommendationRerankValidator.validate(
                    baseCandidates,
                    reranker.rerank(preference.toRerankerPreference(), baseCandidates)
            );

            return topBaseCandidates.stream()
                    .map(candidate -> mergeRerankedRecommendation(
                            candidate,
                            rerankedByCourseId.get(candidate.candidate().courseId())
                    ))
                    .toList();
        } catch (RuntimeException exception) {
            log.warn("Home recommendation AI reranking failed. Falling back to base recommendations.", exception);
            return topBaseCandidates;
        }
    }

    private ScoredRecommendation mergeRerankedRecommendation(
            ScoredRecommendation candidate,
            RerankedRecommendation rerankedRecommendation
    ) {
        if (rerankedRecommendation == null) {
            return candidate;
        }
        return new ScoredRecommendation(
                candidate.candidate(),
                candidate.themeCodes(),
                candidate.userTypes(),
                candidate.distanceFromUserKm(),
                candidate.baseScoreComponents(),
                candidate.baseScore(),
                rerankedRecommendation.ragScore(),
                calculateFinalScore(candidate.baseScore(), rerankedRecommendation.ragScore()),
                rerankedRecommendation.recommendationReason()
        );
    }

    private Map<UUID, String> fallbackReasonsByCourseId(
            UserPreference preference,
            List<ScoredRecommendation> topBaseCandidates
    ) {
        Map<UUID, String> reasons = new LinkedHashMap<>();
        CourseRecommendationContextRetriever contextRetriever =
                courseRecommendationContextRetrieverProvider.getIfAvailable();
        if (contextRetriever != null) {
            try {
                for (RetrievedRecommendationContext context : contextRetriever.retrieve(
                        preference.toRerankerPreference(),
                        topBaseCandidates.stream()
                                .map(ScoredRecommendation::toBaseRecommendationCandidate)
                                .toList()
                )) {
                    if (!reasons.containsKey(context.courseId())) {
                        String reason = evidenceReason(context);
                        if (StringUtils.hasText(reason)) {
                            reasons.put(context.courseId(), reason);
                        }
                    }
                }
            } catch (RuntimeException exception) {
                log.warn("Failed to load recommendation evidence contexts. Falling back to course descriptions.", exception);
            }
        }

        for (ScoredRecommendation candidate : topBaseCandidates) {
            reasons.computeIfAbsent(
                    candidate.candidate().courseId(),
                    ignored -> evidenceReason(
                            new RetrievedRecommendationContext(
                                    "course-description-" + candidate.candidate().courseId(),
                                    candidate.candidate().courseId(),
                                    "COURSE_DESCRIPTION",
                                    candidate.candidate().description()
                            )
                    )
            );
        }
        return reasons;
    }

    private String evidenceReason(RetrievedRecommendationContext context) {
        if (context == null || !StringUtils.hasText(context.content())) {
            return null;
        }

        List<String> evidences = extractEvidencePhrases(context.content());
        if (evidences.isEmpty()) {
            return null;
        }

        if (evidences.size() == 1) {
            String evidence = evidences.get(0);
            return evidence + subjectParticle(evidence) + " 잘 느껴지는 코스예요";
        }
        String first = evidences.get(0);
        String second = evidences.get(1);
        String combinedEvidence = first + connectiveParticle(first) + " " + second;
        return combinedEvidence + subjectParticle(second) + " 잘 느껴지는 코스예요";
    }

    private List<String> extractEvidencePhrases(String content) {
        String normalized = content.toLowerCase();
        List<String> evidences = new ArrayList<>();
        addEvidence(evidences, normalized, "바다 전망", "ocean", "바다", "해안", "노을");
        addEvidence(evidences, normalized, "사진 포인트", "photo", "사진", "포토");
        addEvidence(evidences, normalized, "숲길과 그늘", "forest", "shade", "숲", "그늘");
        addEvidence(evidences, normalized, "오름 조망", "오름", "오르막", "상승", "climb");
        addEvidence(evidences, normalized, "관광 동선", "여행", "관광", "카페", "시장", "마을");
        addEvidence(evidences, normalized, "반복 러닝", "반복", "생활형", "평일", "도심");
        addEvidence(evidences, normalized, "편의시설 접근", "편의시설", "접근성");
        addEvidence(evidences, normalized, "한적한 분위기", "한적", "조용", "quiet", "calm");
        addEvidence(evidences, normalized, "도전적인 코스감", "강도", "훈련", "지구력", "도전");
        return evidences;
    }

    private void addEvidence(List<String> evidences, String content, String phrase, String... keywords) {
        if (evidences.contains(phrase)) {
            return;
        }
        for (String keyword : keywords) {
            if (content.contains(keyword.toLowerCase())) {
                evidences.add(phrase);
                return;
            }
        }
    }

    private String connectiveParticle(String text) {
        return hasFinalConsonant(text) ? "과" : "와";
    }

    private String subjectParticle(String text) {
        return hasFinalConsonant(text) ? "이" : "가";
    }

    private boolean hasFinalConsonant(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        char last = text.trim().charAt(text.trim().length() - 1);
        if (last < '가' || last > '힣') {
            return false;
        }
        return (last - '가') % 28 != 0;
    }

    private double calculateFinalScore(double baseScore, double ragScore) {
        return round(
                (baseScore * homeRecommendationProperties.getBaseScoreWeight()) +
                        (ragScore * homeRecommendationProperties.getRagScoreWeight())
        );
    }

    private double calculateBaseScore(BaseScoreComponents components) {
        return round(components.total());
    }

    private double calculateDistancePreferenceScore(PreferredDistance preferredDistance, BigDecimal distanceKm) {
        if (preferredDistance == null || distanceKm == null) {
            return 0;
        }

        double distance = distanceKm.doubleValue();
        return switch (preferredDistance) {
            case UNDER_3KM -> {
                if (distance <= 3.0) {
                    yield DISTANCE_PREFERENCE_WEIGHT;
                }
                if (distance <= 5.0) {
                    yield DISTANCE_PREFERENCE_SECONDARY_SCORE;
                }
                if (distance <= 8.0) {
                    yield DISTANCE_PREFERENCE_LOW_MATCH_SCORE;
                }
                yield 0;
            }
            case FROM_5_TO_10KM -> {
                if (distance >= 5.0 && distance <= 10.0) {
                    yield DISTANCE_PREFERENCE_WEIGHT;
                }
                if ((distance >= 3.0 && distance < 5.0) || (distance > 10.0 && distance <= 12.0)) {
                    yield DISTANCE_PREFERENCE_NEAR_MATCH_SCORE;
                }
                if (distance > 12.0 && distance <= 15.0) {
                    yield DISTANCE_PREFERENCE_FAR_MATCH_SCORE;
                }
                yield 0;
            }
            case OVER_10KM -> {
                if (distance >= 10.0) {
                    yield DISTANCE_PREFERENCE_WEIGHT;
                }
                if (distance >= 8.0) {
                    yield DISTANCE_PREFERENCE_NEAR_MATCH_SCORE;
                }
                if (distance >= 5.0) {
                    yield DISTANCE_PREFERENCE_MINIMAL_MATCH_SCORE;
                }
                yield 0;
            }
        };
    }

    private double calculateThemeScore(Set<String> preferredThemes, List<String> courseThemes) {
        if (preferredThemes.isEmpty() || courseThemes.isEmpty()) {
            return 0;
        }

        long matched = courseThemes.stream().filter(preferredThemes::contains).count();
        if (matched == 0) {
            return 0;
        }
        return round(THEME_WEIGHT * matched / preferredThemes.size());
    }

    private double calculateDifficultyScore(PreferredDifficulty preferredDifficulty, Difficulty courseDifficulty) {
        if (preferredDifficulty == null || courseDifficulty == null) {
            return 0;
        }

        Difficulty preferredCourseDifficulty = mapToCourseDifficulty(preferredDifficulty);
        int distance = Math.abs(preferredCourseDifficulty.ordinal() - courseDifficulty.ordinal());
        if (distance == 0) {
            return DIFFICULTY_WEIGHT;
        }
        if (distance == 1) {
            return DIFFICULTY_NEAR_MATCH_SCORE;
        }
        return 0;
    }

    private Difficulty mapToCourseDifficulty(PreferredDifficulty preferredDifficulty) {
        return switch (preferredDifficulty) {
            case EASY -> Difficulty.LOW;
            case NORMAL -> Difficulty.MID;
            case HARD -> Difficulty.HIGH;
        };
    }

    private double calculateLocationScore(Double distanceMeters) {
        if (distanceMeters == null) {
            return 0;
        }
        double distanceKm = distanceMeters / 1000.0;
        if (distanceKm <= 3.0) {
            return LOCATION_WEIGHT;
        }
        if (distanceKm <= 10.0) {
            return LOCATION_NEARBY_SCORE;
        }
        if (distanceKm <= 25.0) {
            return LOCATION_ACCESSIBLE_SCORE;
        }
        if (distanceKm <= 50.0) {
            return LOCATION_FAR_SCORE;
        }
        return 0;
    }

    private double calculateUserTypeScore(
            Set<UserTypeCode> userTypes,
            CourseRecommendationCandidate candidate,
            List<String> themeCodes
    ) {
        if (userTypes.isEmpty()) {
            return 0;
        }

        double score = 0;
        for (UserTypeCode userType : userTypes) {
            score += switch (userType) {
                case ACTIVE_RUNNER -> activeRunnerScore(candidate);
                case RELAXED_TRAVELER -> relaxedTravelerScore(candidate, themeCodes);
                case JEJU_RESIDENT -> jejuResidentScore(candidate, themeCodes);
            };
        }
        return Math.min(USER_TYPE_WEIGHT, round(score / userTypes.size()));
    }

    private double activeRunnerScore(CourseRecommendationCandidate candidate) {
        double score = 0;
        if (candidate.courseType() == CourseType.RUNNING_COURSE) {
            score += 2;
        }
        if (candidate.distanceKm().doubleValue() >= 10.0) {
            score += 4;
        } else if (candidate.distanceKm().doubleValue() >= 8.0) {
            score += 2;
        }
        if (candidate.difficulty() == Difficulty.HIGH) {
            score += 3;
        } else if (candidate.difficulty() == Difficulty.MID) {
            score += 2;
        }
        if (candidate.elevationGainM().doubleValue() >= 100.0) {
            score += 1;
        } else if (candidate.elevationGainM().doubleValue() >= 60.0) {
            score += 0.5;
        }
        return score;
    }

    private double relaxedTravelerScore(CourseRecommendationCandidate candidate, List<String> themeCodes) {
        double score = 0;
        double distanceKm = candidate.distanceKm().doubleValue();
        if (distanceKm <= 6.0) {
            score += 3;
        } else if (distanceKm <= 8.0) {
            score += 2;
        }
        if (candidate.difficulty() == Difficulty.LOW) {
            score += 3;
        } else if (candidate.difficulty() == Difficulty.MID) {
            score += 1;
        }
        long matchedThemes = themeCodes.stream().filter(RELAXED_TRAVELER_THEME_CODES::contains).count();
        if (matchedThemes >= 2) {
            score += 4;
        } else if (matchedThemes == 1) {
            score += 2.5;
        }
        if (candidate.courseType() == CourseType.SPOT_COURSE) {
            score += 1;
        }
        return score;
    }

    private double jejuResidentScore(CourseRecommendationCandidate candidate, List<String> themeCodes) {
        double score = 0;
        if (candidate.distanceMeters() != null && candidate.distanceMeters() <= 15_000) {
            score += 4;
        } else if (candidate.distanceMeters() != null && candidate.distanceMeters() <= 30_000) {
            score += 2;
        }
        if (candidate.courseType() == CourseType.RUNNING_COURSE) {
            score += 3;
        }
        double distanceKm = candidate.distanceKm().doubleValue();
        if (distanceKm >= 3.0 && distanceKm <= 10.0) {
            score += 3;
        } else if (distanceKm > 10.0 && distanceKm <= 12.0) {
            score += 1.5;
        }
        if (candidate.difficulty() != Difficulty.HIGH) {
            score += 1;
        }
        if (themeCodes.contains("URBAN") || themeCodes.contains("FOREST")) {
            score += 2;
        }
        return score;
    }

    private double calculatePopularityScore(BigDecimal averageRating, Integer completionCount) {
        double score = 0;
        if (averageRating != null) {
            double rating = averageRating.doubleValue();
            if (rating >= 4.5) {
                score += HIGH_RATING_SCORE;
            } else if (rating >= 4.0) {
                score += MID_RATING_SCORE;
            } else if (rating >= 3.5) {
                score += LOW_RATING_SCORE;
            }
        }
        if (completionCount != null) {
            if (completionCount >= 100) {
                score += HIGH_COMPLETION_SCORE;
            } else if (completionCount >= 30) {
                score += MID_COMPLETION_SCORE;
            }
        }
        return Math.min(POPULARITY_WEIGHT, score);
    }

    private RecommendedCoursesResponse.RecommendedCourseItem toResponseItem(
            ScoredRecommendation recommendation,
            String fallbackReason
    ) {
        return new RecommendedCoursesResponse.RecommendedCourseItem(
                recommendation.candidate().courseId(),
                recommendation.candidate().courseName(),
                recommendation.candidate().distanceKm(),
                recommendation.candidate().difficulty(),
                recommendation.themeCodes(),
                recommendation.candidate().averageRating(),
                recommendation.distanceFromUserKm(),
                recommendation.baseScore(),
                recommendation.ragScore(),
                recommendation.finalScore(),
                StringUtils.hasText(recommendation.recommendationReason())
                        ? recommendation.recommendationReason()
                        : defaultReason(recommendation, fallbackReason)
        );
    }

    private String defaultReason(ScoredRecommendation recommendation, String fallbackReason) {
        if (StringUtils.hasText(fallbackReason)) {
            return fallbackReason;
        }
        return themeFallbackReason(recommendation);
    }

    private Comparator<ScoredRecommendation> baseRecommendationComparator() {
        return Comparator.comparingDouble(ScoredRecommendation::baseScore).reversed()
                .thenComparing(
                        scored -> scored.candidate().averageRating(),
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
                .thenComparing(
                        scored -> scored.candidate().completionCount(),
                        Comparator.nullsLast(Comparator.reverseOrder())
                );
    }

    private Comparator<ScoredRecommendation> finalRecommendationComparator() {
        return Comparator.comparingDouble(ScoredRecommendation::finalScore).reversed()
                .thenComparing(Comparator.comparingDouble(ScoredRecommendation::baseScore).reversed())
                .thenComparing(
                        scored -> scored.candidate().averageRating(),
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
                .thenComparing(
                        scored -> scored.candidate().completionCount(),
                        Comparator.nullsLast(Comparator.reverseOrder())
                );
    }

    private String themeFallbackReason(ScoredRecommendation recommendation) {
        List<String> themeLabels = recommendation.themeCodes().stream()
                .map(THEME_LABELS::get)
                .filter(Objects::nonNull)
                .limit(2)
                .toList();

        if (!themeLabels.isEmpty()) {
            return String.join(", ", themeLabels) + " 분위기가 잘 맞는 코스예요";
        }

        CourseRecommendationCandidate candidate = recommendation.candidate();
        if (candidate.courseType() == CourseType.SPOT_COURSE) {
            return "가볍게 둘러보기 좋은 스팟 코스예요";
        }
        if (recommendation.themeCodes().contains("OREUM")) {
            return "오름 분위기를 느끼기 좋은 코스예요";
        }
        if (recommendation.themeCodes().contains("FOREST")) {
            return "숲길 분위기가 살아 있는 코스예요";
        }
        if (recommendation.themeCodes().contains("COAST")) {
            return "제주 해안을 느끼기 좋은 코스예요";
        }
        if (recommendation.themeCodes().contains("URBAN")) {
            return "도심에서 반복해 뛰기 좋은 코스예요";
        }
        return "선호 조건에 잘 맞는 코스예요";
    }

    private void validateLocation(Double latitude, Double longitude) {
        if (latitude == null && longitude == null) {
            return;
        }
        if (latitude == null || longitude == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Latitude and longitude must be provided together.");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid location coordinates.");
        }
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    private record UserPreference(
            UUID userId,
            PreferredDistance preferredDistance,
            PreferredDifficulty preferredDifficulty,
            Set<UserTypeCode> userTypes,
            Set<String> themeCodes
    ) {
        private RecommendationUserPreference toRerankerPreference() {
            return new RecommendationUserPreference(
                    userId,
                    preferredDistance,
                    preferredDifficulty,
                    userTypes,
                    themeCodes
            );
        }
    }

    private record ScoredRecommendation(
            CourseRecommendationCandidate candidate,
            List<String> themeCodes,
            Set<UserTypeCode> userTypes,
            Double distanceFromUserKm,
            BaseScoreComponents baseScoreComponents,
            double baseScore,
            Double ragScore,
            double finalScore,
            String recommendationReason
    ) {
        private BaseRecommendationCandidate toBaseRecommendationCandidate() {
            return new BaseRecommendationCandidate(
                    candidate.courseId(),
                    candidate.courseName(),
                    candidate.description(),
                    candidate.courseType(),
                    candidate.distanceKm(),
                    candidate.difficulty(),
                    candidate.averageRating(),
                    candidate.completionCount(),
                    themeCodes,
                    distanceFromUserKm,
                    baseScore
            );
        }
    }

    private record BaseScoreComponents(
            double distancePreferenceScore,
            double themeScore,
            double difficultyScore,
            double locationScore,
            double userTypeScore,
            double popularityScore
    ) {
        private double total() {
            return distancePreferenceScore
                    + themeScore
                    + difficultyScore
                    + locationScore
                    + userTypeScore
                    + popularityScore;
        }
    }
}
