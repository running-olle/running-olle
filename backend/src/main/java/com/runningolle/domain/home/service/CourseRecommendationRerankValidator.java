package com.runningolle.domain.home.service;

import com.runningolle.domain.home.service.CourseRecommendationReranker.BaseRecommendationCandidate;
import com.runningolle.domain.home.service.CourseRecommendationReranker.RerankedRecommendation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CourseRecommendationRerankValidator {

    private static final double MIN_SCORE = 0.0;
    private static final double MAX_SCORE = 100.0;
    private static final double MAX_USER_TYPE_FIT = 20.0;
    private static final double MAX_THEME_FIT = 25.0;
    private static final double MAX_REVIEW_SATISFACTION = 20.0;
    private static final double MAX_SEASON_FIT = 15.0;
    private static final double MAX_SAFETY = 10.0;
    private static final double MAX_CONVENIENCE = 10.0;

    public Map<UUID, RerankedRecommendation> validate(
            List<BaseRecommendationCandidate> baseCandidates,
            List<RerankedRecommendation> rerankedRecommendations
    ) {
        if (rerankedRecommendations == null || rerankedRecommendations.isEmpty()) {
            return Map.of();
        }

        Set<UUID> candidateIds = baseCandidates.stream()
                .map(BaseRecommendationCandidate::courseId)
                .collect(Collectors.toSet());

        Map<UUID, RerankedRecommendation> validRecommendations = new LinkedHashMap<>();
        for (RerankedRecommendation rerankedRecommendation : rerankedRecommendations) {
            if (rerankedRecommendation == null || rerankedRecommendation.courseId() == null) {
                log.warn("Ignoring empty reranked recommendation.");
                continue;
            }
            if (!candidateIds.contains(rerankedRecommendation.courseId())) {
                log.warn("Ignoring reranked recommendation for non-candidate courseId={}", rerankedRecommendation.courseId());
                continue;
            }
            if (!isValidScore(rerankedRecommendation.ragScore())) {
                log.warn(
                        "Ignoring reranked recommendation with invalid ragScore. courseId={}, ragScore={}",
                        rerankedRecommendation.courseId(),
                        rerankedRecommendation.ragScore()
                );
                continue;
            }
            if (!isValidBreakdown(rerankedRecommendation.scores())) {
                log.warn(
                        "Ignoring reranked recommendation with invalid score breakdown. courseId={}",
                        rerankedRecommendation.courseId()
                );
                continue;
            }
            if (Double.compare(round(rerankedRecommendation.scores().total()), round(rerankedRecommendation.ragScore())) != 0) {
                log.warn(
                        "Ignoring reranked recommendation with mismatched ragScore total. courseId={}, ragScore={}, breakdownTotal={}",
                        rerankedRecommendation.courseId(),
                        rerankedRecommendation.ragScore(),
                        rerankedRecommendation.scores().total()
                );
                continue;
            }
            validRecommendations.putIfAbsent(rerankedRecommendation.courseId(), rerankedRecommendation);
        }
        return validRecommendations;
    }

    private boolean isValidScore(double score) {
        return score >= MIN_SCORE && score <= MAX_SCORE;
    }

    private boolean isValidBreakdown(CourseRecommendationReranker.RagScoreBreakdown scores) {
        return scores != null
                && isValidRange(scores.userTypeFit(), MAX_USER_TYPE_FIT)
                && isValidRange(scores.themeFit(), MAX_THEME_FIT)
                && isValidRange(scores.reviewSatisfaction(), MAX_REVIEW_SATISFACTION)
                && isValidRange(scores.seasonFit(), MAX_SEASON_FIT)
                && isValidRange(scores.safety(), MAX_SAFETY)
                && isValidRange(scores.convenience(), MAX_CONVENIENCE);
    }

    private boolean isValidRange(double score, double max) {
        return score >= MIN_SCORE && score <= max;
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
