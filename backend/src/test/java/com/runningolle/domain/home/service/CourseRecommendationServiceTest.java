package com.runningolle.domain.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.entity.CourseTheme;
import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.course.repository.CourseThemeRepository;
import com.runningolle.domain.home.config.HomeRecommendationProperties;
import com.runningolle.domain.home.dto.RecommendedCoursesResponse;
import com.runningolle.domain.home.repository.CourseRecommendationQueryRepository;
import com.runningolle.domain.home.repository.CourseRecommendationQueryRepository.CourseRecommendationCandidate;
import com.runningolle.domain.home.service.CourseRecommendationReranker.RagScoreBreakdown;
import com.runningolle.domain.home.service.CourseRecommendationReranker.RerankedRecommendation;
import com.runningolle.domain.user.entity.Theme;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.entity.UserTheme;
import com.runningolle.domain.user.entity.UserType;
import com.runningolle.domain.user.entity.UserUserType;
import com.runningolle.domain.user.enums.PreferredDifficulty;
import com.runningolle.domain.user.enums.PreferredDistance;
import com.runningolle.domain.user.repository.UserRepository;
import com.runningolle.domain.user.repository.UserThemeRepository;
import com.runningolle.domain.user.repository.UserUserTypeRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CourseRecommendationServiceTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserUserTypeRepository userUserTypeRepository;

    @Mock
    private UserThemeRepository userThemeRepository;

    @Mock
    private CourseThemeRepository courseThemeRepository;

    @Mock
    private CourseRecommendationQueryRepository courseRecommendationQueryRepository;

    @Mock
    private ObjectProvider<CourseRecommendationReranker> courseRecommendationRerankerProvider;

    @Mock
    private ObjectProvider<CourseRecommendationContextRetriever> courseRecommendationContextRetrieverProvider;

    @Mock
    private CourseRecommendationContextRetriever courseRecommendationContextRetriever;

    @Mock
    private CourseRecommendationReranker courseRecommendationReranker;

    private HomeRecommendationProperties homeRecommendationProperties;
    private CourseRecommendationRerankValidator courseRecommendationRerankValidator;
    private CourseRecommendationService courseRecommendationService;

    @BeforeEach
    void setUp() {
        homeRecommendationProperties = new HomeRecommendationProperties();
        courseRecommendationRerankValidator = new CourseRecommendationRerankValidator();
        courseRecommendationService = new CourseRecommendationService(
                userRepository,
                userUserTypeRepository,
                userThemeRepository,
                courseThemeRepository,
                courseRecommendationQueryRepository,
                homeRecommendationProperties,
                courseRecommendationRerankerProvider,
                courseRecommendationContextRetrieverProvider,
                courseRecommendationRerankValidator
        );
    }

    @Test
    void returnsTopThreeRecommendationsOrderedByBaseScore() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, PreferredDistance.FROM_5_TO_10KM, PreferredDifficulty.NORMAL);
        Theme coast = theme("COAST");
        Theme photo = theme("PHOTO");

        Course course1 = course(UUID.randomUUID(), "A");
        Course course2 = course(UUID.randomUUID(), "B");
        Course course3 = course(UUID.randomUUID(), "C");
        Course course4 = course(UUID.randomUUID(), "D");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userUserTypeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserUserType.of(user, UserType.of("RELAXED_TRAVELER", "Relaxed traveler"))));
        given(userThemeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserTheme.of(user, coast), UserTheme.of(user, photo)));
        given(courseRecommendationQueryRepository.findRecommendationCandidates(33.45, 126.57))
                .willReturn(List.of(
                        candidate(course1.getId(), "A", new BigDecimal("6.0"), Difficulty.LOW, new BigDecimal("4.8"), 120, 1_000.0),
                        candidate(course2.getId(), "B", new BigDecimal("11.0"), Difficulty.HIGH, new BigDecimal("4.7"), 90, 1_200.0),
                        candidate(course3.getId(), "C", new BigDecimal("5.5"), Difficulty.MID, new BigDecimal("4.3"), 40, 8_000.0),
                        candidate(course4.getId(), "D", new BigDecimal("2.5"), Difficulty.LOW, new BigDecimal("3.8"), 10, 2_000.0)
                ));
        given(courseThemeRepository.findAllByCourse_IdIn(List.of(course1.getId(), course2.getId(), course3.getId(), course4.getId())))
                .willReturn(List.of(
                        CourseTheme.of(course1, coast),
                        CourseTheme.of(course1, photo),
                        CourseTheme.of(course2, coast),
                        CourseTheme.of(course3, photo)
                ));

        RecommendedCoursesResponse response = courseRecommendationService.getRecommendedCourses(userId, 33.45, 126.57);

        assertThat(response.recommendations()).hasSize(3);
        assertThat(response.recommendations())
                .extracting(RecommendedCoursesResponse.RecommendedCourseItem::courseName)
                .containsExactly("A", "C", "B");
        assertThat(response.recommendations().get(0).distanceFromUserKm()).isEqualTo(1.0);
        assertThat(response.recommendations().get(0).ragScore()).isNull();
        assertThat(response.recommendations().get(0).finalScore())
                .isEqualTo(response.recommendations().get(0).baseScore());
        assertThat(response.recommendations().get(0).baseScore()).isGreaterThan(response.recommendations().get(1).baseScore());
        verify(courseRecommendationRerankerProvider, never()).getIfAvailable();
    }

    @Test
    void rejectsHalfProvidedLocation() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> courseRecommendationService.getRecommendedCourses(userId, 33.45, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Latitude and longitude");
    }

    @Test
    void rejectsInvalidCoordinates() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> courseRecommendationService.getRecommendedCourses(userId, 91.0, 126.57))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid location coordinates");

        assertThatThrownBy(() -> courseRecommendationService.getRecommendedCourses(userId, 33.45, 181.0))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid location coordinates");
    }

    @Test
    void returnsRecommendationsWithoutLocation() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, PreferredDistance.UNDER_3KM, PreferredDifficulty.EASY);
        Theme coast = theme("COAST");

        Course course1 = course(UUID.randomUUID(), "Near Coast");
        Course course2 = course(UUID.randomUUID(), "Town Loop");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userUserTypeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserUserType.of(user, UserType.of("RELAXED_TRAVELER", "Relaxed traveler"))));
        given(userThemeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserTheme.of(user, coast)));
        given(courseRecommendationQueryRepository.findRecommendationCandidates(null, null))
                .willReturn(List.of(
                        candidate(course1.getId(), "Near Coast", new BigDecimal("2.8"), Difficulty.LOW, new BigDecimal("4.6"), 35, null),
                        candidate(course2.getId(), "Town Loop", new BigDecimal("5.2"), Difficulty.MID, new BigDecimal("4.1"), 40, null)
                ));
        given(courseThemeRepository.findAllByCourse_IdIn(List.of(course1.getId(), course2.getId())))
                .willReturn(List.of(CourseTheme.of(course1, coast)));

        RecommendedCoursesResponse response = courseRecommendationService.getRecommendedCourses(userId, null, null);

        assertThat(response.recommendations()).hasSize(2);
        assertThat(response.recommendations())
                .extracting(RecommendedCoursesResponse.RecommendedCourseItem::distanceFromUserKm)
                .containsExactly(null, null);
        assertThat(response.recommendations().get(0).courseName()).isEqualTo("Near Coast");
        assertThat(response.recommendations().get(0).recommendationReason()).contains("해안 풍경");
        assertThat(response.recommendations().get(0).recommendationReason()).doesNotContain("선호 거리");
    }

    @Test
    void usesReviewContextForFallbackRecommendationReasonWhenAiIsDisabled() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, PreferredDistance.FROM_5_TO_10KM, PreferredDifficulty.EASY);
        Theme coast = theme("COAST");
        Course course = course(UUID.randomUUID(), "Review Rich Course");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userUserTypeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserUserType.of(user, UserType.of("RELAXED_TRAVELER", "Relaxed traveler"))));
        given(userThemeRepository.findAllByUserId(userId)).willReturn(List.of(UserTheme.of(user, coast)));
        given(courseRecommendationQueryRepository.findRecommendationCandidates(null, null))
                .willReturn(List.of(candidate(
                        course.getId(),
                        "Review Rich Course",
                        new BigDecimal("6.0"),
                        Difficulty.LOW,
                        new BigDecimal("4.6"),
                        50,
                        null
                )));
        given(courseThemeRepository.findAllByCourse_IdIn(List.of(course.getId())))
                .willReturn(List.of(CourseTheme.of(course, coast)));
        given(courseRecommendationContextRetrieverProvider.getIfAvailable()).willReturn(courseRecommendationContextRetriever);
        given(courseRecommendationContextRetriever.retrieve(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList()))
                .willReturn(List.of(new CourseRecommendationContextRetriever.RetrievedRecommendationContext(
                        "review-doc",
                        course.getId(),
                        "COURSE_REVIEW",
                        "노을 시간대 바다 전망이 좋고 사진 찍기 좋은 포인트가 많았다."
                )));

        RecommendedCoursesResponse response = courseRecommendationService.getRecommendedCourses(userId, null, null);

        assertThat(response.recommendations()).singleElement()
                .satisfies(recommendation -> {
                    assertThat(recommendation.ragScore()).isNull();
                    assertThat(recommendation.recommendationReason())
                            .isEqualTo("바다 전망과 사진 포인트가 잘 느껴지는 코스예요");
                    assertThat(recommendation.recommendationReason()).doesNotContain("6.0");
                    assertThat(recommendation.recommendationReason()).doesNotContain("난이도");
                });
    }

    @Test
    void favorsLongAndHardCoursesForActiveRunner() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, PreferredDistance.OVER_10KM, PreferredDifficulty.HARD);
        Theme oreum = theme("OREUM");
        Theme forest = theme("FOREST");
        Theme coast = theme("COAST");

        Course course1 = course(UUID.randomUUID(), "Long Oreum Course");
        Course course2 = course(UUID.randomUUID(), "Short Coast Course");
        Course course3 = course(UUID.randomUUID(), "Mid Forest Course");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userUserTypeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserUserType.of(user, UserType.of("ACTIVE_RUNNER", "Active runner"))));
        given(userThemeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserTheme.of(user, oreum), UserTheme.of(user, forest)));
        given(courseRecommendationQueryRepository.findRecommendationCandidates(null, null))
                .willReturn(List.of(
                        candidate(course1.getId(), "Long Oreum Course", CourseType.RUNNING_COURSE, new BigDecimal("14.0"),
                                Difficulty.HIGH, new BigDecimal("4.5"), 80, null, new BigDecimal("160.0")),
                        candidate(course2.getId(), "Short Coast Course", CourseType.RUNNING_COURSE, new BigDecimal("5.0"),
                                Difficulty.LOW, new BigDecimal("4.9"), 140, null, new BigDecimal("12.0")),
                        candidate(course3.getId(), "Mid Forest Course", CourseType.RUNNING_COURSE, new BigDecimal("8.5"),
                                Difficulty.MID, new BigDecimal("4.4"), 60, null, new BigDecimal("70.0"))
                ));
        given(courseThemeRepository.findAllByCourse_IdIn(List.of(course1.getId(), course2.getId(), course3.getId())))
                .willReturn(List.of(
                        CourseTheme.of(course1, oreum),
                        CourseTheme.of(course1, forest),
                        CourseTheme.of(course2, coast),
                        CourseTheme.of(course3, forest)
                ));

        RecommendedCoursesResponse response = courseRecommendationService.getRecommendedCourses(userId, null, null);

        assertThat(response.recommendations())
                .extracting(RecommendedCoursesResponse.RecommendedCourseItem::courseName)
                .containsExactly("Long Oreum Course", "Mid Forest Course", "Short Coast Course");
    }

    @Test
    void favorsNearbyRepeatableCoursesForJejuResident() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, PreferredDistance.FROM_5_TO_10KM, PreferredDifficulty.NORMAL);
        Theme urban = theme("URBAN");
        Theme forest = theme("FOREST");
        Theme oreum = theme("OREUM");
        Theme coast = theme("COAST");

        Course course1 = course(UUID.randomUUID(), "City Repeat Course");
        Course course2 = course(UUID.randomUUID(), "Far Oreum Course");
        Course course3 = course(UUID.randomUUID(), "Mid Coast Course");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userUserTypeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserUserType.of(user, UserType.of("JEJU_RESIDENT", "Jeju resident"))));
        given(userThemeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserTheme.of(user, urban), UserTheme.of(user, forest)));
        given(courseRecommendationQueryRepository.findRecommendationCandidates(33.4996, 126.5312))
                .willReturn(List.of(
                        candidate(course1.getId(), "City Repeat Course", CourseType.RUNNING_COURSE, new BigDecimal("6.0"),
                                Difficulty.MID, new BigDecimal("4.3"), 110, 1_200.0, new BigDecimal("24.0")),
                        candidate(course2.getId(), "Far Oreum Course", CourseType.RUNNING_COURSE, new BigDecimal("11.5"),
                                Difficulty.HIGH, new BigDecimal("4.7"), 130, 42_000.0, new BigDecimal("180.0")),
                        candidate(course3.getId(), "Mid Coast Course", CourseType.RUNNING_COURSE, new BigDecimal("7.0"),
                                Difficulty.LOW, new BigDecimal("4.6"), 90, 18_000.0, new BigDecimal("20.0"))
                ));
        given(courseThemeRepository.findAllByCourse_IdIn(List.of(course1.getId(), course2.getId(), course3.getId())))
                .willReturn(List.of(
                        CourseTheme.of(course1, urban),
                        CourseTheme.of(course1, forest),
                        CourseTheme.of(course2, oreum),
                        CourseTheme.of(course3, coast)
                ));

        RecommendedCoursesResponse response = courseRecommendationService.getRecommendedCourses(userId, 33.4996, 126.5312);

        assertThat(response.recommendations())
                .extracting(RecommendedCoursesResponse.RecommendedCourseItem::courseName)
                .containsExactly("City Repeat Course", "Mid Coast Course", "Far Oreum Course");
    }

    @Test
    void keepsOnlyTopThreeFromTopFiveInternalCandidates() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, PreferredDistance.FROM_5_TO_10KM, PreferredDifficulty.NORMAL);
        Theme coast = theme("COAST");
        Theme photo = theme("PHOTO");

        Course course1 = course(UUID.randomUUID(), "A");
        Course course2 = course(UUID.randomUUID(), "B");
        Course course3 = course(UUID.randomUUID(), "C");
        Course course4 = course(UUID.randomUUID(), "D");
        Course course5 = course(UUID.randomUUID(), "E");
        Course course6 = course(UUID.randomUUID(), "F");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userUserTypeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserUserType.of(user, UserType.of("RELAXED_TRAVELER", "Relaxed traveler"))));
        given(userThemeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserTheme.of(user, coast), UserTheme.of(user, photo)));
        given(courseRecommendationQueryRepository.findRecommendationCandidates(33.45, 126.57))
                .willReturn(List.of(
                        candidate(course1.getId(), "A", new BigDecimal("6.0"), Difficulty.LOW, new BigDecimal("4.8"), 120, 1_000.0),
                        candidate(course2.getId(), "B", new BigDecimal("11.0"), Difficulty.HIGH, new BigDecimal("4.7"), 90, 1_200.0),
                        candidate(course3.getId(), "C", new BigDecimal("5.5"), Difficulty.MID, new BigDecimal("4.3"), 40, 8_000.0),
                        candidate(course4.getId(), "D", new BigDecimal("2.5"), Difficulty.LOW, new BigDecimal("3.8"), 10, 2_000.0),
                        candidate(course5.getId(), "E", new BigDecimal("9.0"), Difficulty.LOW, new BigDecimal("4.0"), 25, 4_500.0),
                        candidate(course6.getId(), "F", new BigDecimal("20.0"), Difficulty.HIGH, new BigDecimal("2.5"), 5, 60_000.0)
                ));
        given(courseThemeRepository.findAllByCourse_IdIn(List.of(
                course1.getId(), course2.getId(), course3.getId(), course4.getId(), course5.getId(), course6.getId()
        ))).willReturn(List.of(
                CourseTheme.of(course1, coast),
                CourseTheme.of(course1, photo),
                CourseTheme.of(course2, coast),
                CourseTheme.of(course3, photo),
                CourseTheme.of(course5, coast)
        ));

        RecommendedCoursesResponse response = courseRecommendationService.getRecommendedCourses(userId, 33.45, 126.57);

        assertThat(response.recommendations()).hasSize(3);
        assertThat(response.recommendations())
                .extracting(RecommendedCoursesResponse.RecommendedCourseItem::courseName)
                .doesNotContain("F")
                .containsExactly("A", "C", "E");
    }

    @Test
    void returnsEmptyWhenNoRecommendationCandidatesExist() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, PreferredDistance.FROM_5_TO_10KM, PreferredDifficulty.NORMAL);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userUserTypeRepository.findAllByUserId(userId)).willReturn(List.of());
        given(userThemeRepository.findAllByUserId(userId)).willReturn(List.of());
        given(courseRecommendationQueryRepository.findRecommendationCandidates(null, null)).willReturn(List.of());

        RecommendedCoursesResponse response = courseRecommendationService.getRecommendedCourses(userId, null, null);

        assertThat(response.recommendations()).isEmpty();
    }

    @Test
    void usesRerankedScoresWhenAiRerankerSucceeds() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, PreferredDistance.FROM_5_TO_10KM, PreferredDifficulty.NORMAL);
        Theme coast = theme("COAST");

        Course course1 = course(UUID.randomUUID(), "A");
        Course course2 = course(UUID.randomUUID(), "B");
        Course course3 = course(UUID.randomUUID(), "C");

        homeRecommendationProperties.setAiEnabled(true);
        given(courseRecommendationRerankerProvider.getIfAvailable()).willReturn(courseRecommendationReranker);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userUserTypeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserUserType.of(user, UserType.of("RELAXED_TRAVELER", "Relaxed traveler"))));
        given(userThemeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserTheme.of(user, coast)));
        given(courseRecommendationQueryRepository.findRecommendationCandidates(33.45, 126.57))
                .willReturn(List.of(
                        candidate(course1.getId(), "A", new BigDecimal("6.0"), Difficulty.MID, new BigDecimal("4.8"), 120, 1_000.0),
                        candidate(course2.getId(), "B", new BigDecimal("5.5"), Difficulty.LOW, new BigDecimal("4.1"), 40, 2_000.0),
                        candidate(course3.getId(), "C", new BigDecimal("7.0"), Difficulty.MID, new BigDecimal("4.3"), 50, 3_000.0)
                ));
        given(courseThemeRepository.findAllByCourse_IdIn(List.of(course1.getId(), course2.getId(), course3.getId())))
                .willReturn(List.of(
                        CourseTheme.of(course1, coast),
                        CourseTheme.of(course2, coast)
                ));
        given(courseRecommendationReranker.rerank(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList()))
                .willReturn(List.of(
                        new RerankedRecommendation(
                                course2.getId(),
                                new RagScoreBreakdown(18.0, 24.0, 18.0, 12.0, 10.0, 10.0),
                                92.0,
                                "AI reason for B"
                        ),
                        new RerankedRecommendation(
                                course1.getId(),
                                new RagScoreBreakdown(14.0, 18.0, 14.0, 10.0, 7.0, 7.0),
                                70.0,
                                "AI reason for A"
                        )
                ));

        RecommendedCoursesResponse response = courseRecommendationService.getRecommendedCourses(userId, 33.45, 126.57);

        assertThat(response.recommendations())
                .extracting(RecommendedCoursesResponse.RecommendedCourseItem::courseName)
                .containsExactly("A", "B", "C");
        assertThat(response.recommendations().get(1).ragScore()).isEqualTo(92.0);
        assertThat(response.recommendations().get(1).finalScore()).isEqualTo(87.875);
        assertThat(response.recommendations().get(1).recommendationReason()).isEqualTo("AI reason for B");
        assertThat(response.recommendations().get(2).ragScore()).isNull();
        assertThat(response.recommendations().get(2).finalScore())
                .isEqualTo(response.recommendations().get(2).baseScore());

        ArgumentCaptor<List> candidatesCaptor = ArgumentCaptor.forClass(List.class);
        verify(courseRecommendationReranker).rerank(org.mockito.ArgumentMatchers.any(), candidatesCaptor.capture());
        assertThat(candidatesCaptor.getValue()).hasSize(3);
    }

    @Test
    void returnsTopThreeOrderedByFinalScoreAfterReranking() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, PreferredDistance.FROM_5_TO_10KM, PreferredDifficulty.NORMAL);
        Theme coast = theme("COAST");

        Course course1 = course(UUID.randomUUID(), "A");
        Course course2 = course(UUID.randomUUID(), "B");
        Course course3 = course(UUID.randomUUID(), "C");
        Course course4 = course(UUID.randomUUID(), "D");
        Course course5 = course(UUID.randomUUID(), "E");

        homeRecommendationProperties.setAiEnabled(true);
        given(courseRecommendationRerankerProvider.getIfAvailable()).willReturn(courseRecommendationReranker);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userUserTypeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserUserType.of(user, UserType.of("RELAXED_TRAVELER", "Relaxed traveler"))));
        given(userThemeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserTheme.of(user, coast)));
        given(courseRecommendationQueryRepository.findRecommendationCandidates(null, null))
                .willReturn(List.of(
                        candidate(course1.getId(), "A", new BigDecimal("6.0"), Difficulty.LOW, new BigDecimal("4.5"), 30, null),
                        candidate(course2.getId(), "B", new BigDecimal("6.0"), Difficulty.LOW, new BigDecimal("4.5"), 30, null),
                        candidate(course3.getId(), "C", new BigDecimal("6.0"), Difficulty.LOW, new BigDecimal("4.5"), 30, null),
                        candidate(course4.getId(), "D", new BigDecimal("6.0"), Difficulty.LOW, new BigDecimal("4.5"), 30, null),
                        candidate(course5.getId(), "E", new BigDecimal("6.0"), Difficulty.LOW, new BigDecimal("4.5"), 30, null)
                ));
        given(courseThemeRepository.findAllByCourse_IdIn(List.of(
                course1.getId(), course2.getId(), course3.getId(), course4.getId(), course5.getId()
        ))).willReturn(List.of(
                CourseTheme.of(course1, coast),
                CourseTheme.of(course2, coast),
                CourseTheme.of(course3, coast),
                CourseTheme.of(course4, coast),
                CourseTheme.of(course5, coast)
        ));
        given(courseRecommendationReranker.rerank(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList()))
                .willReturn(List.of(
                        new RerankedRecommendation(
                                course5.getId(),
                                new RagScoreBreakdown(20.0, 25.0, 20.0, 15.0, 10.0, 10.0),
                                100.0,
                                "AI reason for E"
                        ),
                        new RerankedRecommendation(
                                course4.getId(),
                                new RagScoreBreakdown(18.0, 25.0, 20.0, 15.0, 10.0, 10.0),
                                98.0,
                                "AI reason for D"
                        ),
                        new RerankedRecommendation(
                                course3.getId(),
                                new RagScoreBreakdown(16.0, 25.0, 20.0, 15.0, 10.0, 10.0),
                                96.0,
                                "AI reason for C"
                        ),
                        new RerankedRecommendation(
                                course2.getId(),
                                new RagScoreBreakdown(10.0, 20.0, 20.0, 10.0, 10.0, 10.0),
                                80.0,
                                "AI reason for B"
                        ),
                        new RerankedRecommendation(
                                course1.getId(),
                                new RagScoreBreakdown(8.0, 20.0, 20.0, 10.0, 10.0, 10.0),
                                78.0,
                                "AI reason for A"
                        )
                ));

        RecommendedCoursesResponse response = courseRecommendationService.getRecommendedCourses(userId, null, null);

        assertThat(response.recommendations()).hasSize(3);
        assertThat(response.recommendations())
                .extracting(RecommendedCoursesResponse.RecommendedCourseItem::courseName)
                .containsExactly("E", "D", "C");
        assertThat(response.recommendations())
                .extracting(RecommendedCoursesResponse.RecommendedCourseItem::recommendationReason)
                .containsExactly("AI reason for E", "AI reason for D", "AI reason for C");
    }

    @Test
    void fallsBackToDefaultReasonWhenAiReasonIsBlank() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, PreferredDistance.FROM_5_TO_10KM, PreferredDifficulty.NORMAL);
        Theme coast = theme("COAST");

        Course course = course(UUID.randomUUID(), "A");

        homeRecommendationProperties.setAiEnabled(true);
        given(courseRecommendationRerankerProvider.getIfAvailable()).willReturn(courseRecommendationReranker);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userUserTypeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserUserType.of(user, UserType.of("RELAXED_TRAVELER", "Relaxed traveler"))));
        given(userThemeRepository.findAllByUserId(userId)).willReturn(List.of(UserTheme.of(user, coast)));
        given(courseRecommendationQueryRepository.findRecommendationCandidates(null, null))
                .willReturn(List.of(candidate(course.getId(), "A", new BigDecimal("6.0"), Difficulty.LOW, new BigDecimal("4.5"), 30, null)));
        given(courseThemeRepository.findAllByCourse_IdIn(List.of(course.getId())))
                .willReturn(List.of(CourseTheme.of(course, coast)));
        given(courseRecommendationReranker.rerank(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList()))
                .willReturn(List.of(new RerankedRecommendation(
                        course.getId(),
                        new RagScoreBreakdown(20.0, 25.0, 20.0, 15.0, 10.0, 10.0),
                        100.0,
                        " "
                )));

        RecommendedCoursesResponse response = courseRecommendationService.getRecommendedCourses(userId, null, null);

        assertThat(response.recommendations()).singleElement()
                .satisfies(recommendation -> {
                    assertThat(recommendation.ragScore()).isEqualTo(100.0);
                    assertThat(recommendation.recommendationReason()).contains("해안 풍경");
                });
    }

    @Test
    void skipsAiRerankingWhenNestedAiRerankFlagIsDisabled() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, PreferredDistance.FROM_5_TO_10KM, PreferredDifficulty.NORMAL);
        Theme coast = theme("COAST");

        homeRecommendationProperties.setAiEnabled(true);
        homeRecommendationProperties.getAi().setRerankEnabled(false);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userUserTypeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserUserType.of(user, UserType.of("RELAXED_TRAVELER", "Relaxed traveler"))));
        given(userThemeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserTheme.of(user, coast)));
        given(courseRecommendationQueryRepository.findRecommendationCandidates(null, null))
                .willReturn(List.of(
                        candidate(UUID.randomUUID(), "A", new BigDecimal("6.0"), Difficulty.LOW, new BigDecimal("4.8"), 120, null),
                        candidate(UUID.randomUUID(), "B", new BigDecimal("8.0"), Difficulty.MID, new BigDecimal("4.2"), 30, null)
                ));
        given(courseThemeRepository.findAllByCourse_IdIn(org.mockito.ArgumentMatchers.anyList()))
                .willReturn(List.of());

        RecommendedCoursesResponse response = courseRecommendationService.getRecommendedCourses(userId, null, null);

        assertThat(response.recommendations()).hasSize(2);
        assertThat(response.recommendations())
                .extracting(RecommendedCoursesResponse.RecommendedCourseItem::ragScore)
                .containsExactly(null, null);
        verify(courseRecommendationRerankerProvider, never()).getIfAvailable();
    }

    @Test
    void passesOnlyTopFiveBaseCandidatesToAiReranker() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, PreferredDistance.FROM_5_TO_10KM, PreferredDifficulty.NORMAL);
        Theme coast = theme("COAST");
        Theme photo = theme("PHOTO");

        Course course1 = course(UUID.randomUUID(), "A");
        Course course2 = course(UUID.randomUUID(), "B");
        Course course3 = course(UUID.randomUUID(), "C");
        Course course4 = course(UUID.randomUUID(), "D");
        Course course5 = course(UUID.randomUUID(), "E");
        Course course6 = course(UUID.randomUUID(), "F");

        homeRecommendationProperties.setAiEnabled(true);
        given(courseRecommendationRerankerProvider.getIfAvailable()).willReturn(courseRecommendationReranker);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userUserTypeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserUserType.of(user, UserType.of("RELAXED_TRAVELER", "Relaxed traveler"))));
        given(userThemeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserTheme.of(user, coast), UserTheme.of(user, photo)));
        given(courseRecommendationQueryRepository.findRecommendationCandidates(33.45, 126.57))
                .willReturn(List.of(
                        candidate(course1.getId(), "A", new BigDecimal("6.0"), Difficulty.LOW, new BigDecimal("4.8"), 120, 1_000.0),
                        candidate(course2.getId(), "B", new BigDecimal("11.0"), Difficulty.HIGH, new BigDecimal("4.7"), 90, 1_200.0),
                        candidate(course3.getId(), "C", new BigDecimal("5.5"), Difficulty.MID, new BigDecimal("4.3"), 40, 8_000.0),
                        candidate(course4.getId(), "D", new BigDecimal("2.5"), Difficulty.LOW, new BigDecimal("3.8"), 10, 2_000.0),
                        candidate(course5.getId(), "E", new BigDecimal("9.0"), Difficulty.LOW, new BigDecimal("4.0"), 25, 4_500.0),
                        candidate(course6.getId(), "F", new BigDecimal("20.0"), Difficulty.HIGH, new BigDecimal("2.5"), 5, 60_000.0)
                ));
        given(courseThemeRepository.findAllByCourse_IdIn(List.of(
                course1.getId(), course2.getId(), course3.getId(), course4.getId(), course5.getId(), course6.getId()
        ))).willReturn(List.of(
                CourseTheme.of(course1, coast),
                CourseTheme.of(course1, photo),
                CourseTheme.of(course2, coast),
                CourseTheme.of(course3, photo),
                CourseTheme.of(course5, coast)
        ));
        given(courseRecommendationReranker.rerank(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList()))
                .willReturn(List.of());

        courseRecommendationService.getRecommendedCourses(userId, 33.45, 126.57);

        ArgumentCaptor<List> candidatesCaptor = ArgumentCaptor.forClass(List.class);
        verify(courseRecommendationReranker).rerank(org.mockito.ArgumentMatchers.any(), candidatesCaptor.capture());
        assertThat(candidatesCaptor.getValue()).hasSize(5);
        assertThat(candidatesCaptor.getValue())
                .extracting("courseName")
                .doesNotContain("F");
    }

    @Test
    void fallsBackToBaseRecommendationsWhenAiRerankerFails() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, PreferredDistance.FROM_5_TO_10KM, PreferredDifficulty.NORMAL);
        Theme coast = theme("COAST");

        Course course1 = course(UUID.randomUUID(), "A");
        Course course2 = course(UUID.randomUUID(), "B");

        homeRecommendationProperties.setAiEnabled(true);
        given(courseRecommendationRerankerProvider.getIfAvailable()).willReturn(courseRecommendationReranker);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userUserTypeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserUserType.of(user, UserType.of("RELAXED_TRAVELER", "Relaxed traveler"))));
        given(userThemeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserTheme.of(user, coast)));
        given(courseRecommendationQueryRepository.findRecommendationCandidates(null, null))
                .willReturn(List.of(
                        candidate(course1.getId(), "A", new BigDecimal("6.0"), Difficulty.LOW, new BigDecimal("4.8"), 120, null),
                        candidate(course2.getId(), "B", new BigDecimal("9.0"), Difficulty.MID, new BigDecimal("4.1"), 40, null)
                ));
        given(courseThemeRepository.findAllByCourse_IdIn(List.of(course1.getId(), course2.getId())))
                .willReturn(List.of(CourseTheme.of(course1, coast)));
        given(courseRecommendationReranker.rerank(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList()))
                .willThrow(new IllegalStateException("reranker failed"));

        RecommendedCoursesResponse response = courseRecommendationService.getRecommendedCourses(userId, null, null);

        assertThat(response.recommendations())
                .extracting(RecommendedCoursesResponse.RecommendedCourseItem::courseName)
                .containsExactly("A", "B");
        assertThat(response.recommendations())
                .extracting(RecommendedCoursesResponse.RecommendedCourseItem::ragScore)
                .containsExactly(null, null);
        assertThat(response.recommendations().get(0).finalScore())
                .isEqualTo(response.recommendations().get(0).baseScore());
    }

    @Test
    void ignoresInvalidRerankedRecommendationsAndKeepsBaseRecommendationForThoseCourses() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, PreferredDistance.FROM_5_TO_10KM, PreferredDifficulty.NORMAL);
        Theme coast = theme("COAST");

        Course course1 = course(UUID.randomUUID(), "A");
        Course course2 = course(UUID.randomUUID(), "B");
        Course course3 = course(UUID.randomUUID(), "C");

        homeRecommendationProperties.setAiEnabled(true);
        given(courseRecommendationRerankerProvider.getIfAvailable()).willReturn(courseRecommendationReranker);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userUserTypeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserUserType.of(user, UserType.of("RELAXED_TRAVELER", "Relaxed traveler"))));
        given(userThemeRepository.findAllByUserId(userId))
                .willReturn(List.of(UserTheme.of(user, coast)));
        given(courseRecommendationQueryRepository.findRecommendationCandidates(null, null))
                .willReturn(List.of(
                        candidate(course1.getId(), "A", new BigDecimal("6.0"), Difficulty.LOW, new BigDecimal("4.8"), 120, null),
                        candidate(course2.getId(), "B", new BigDecimal("7.0"), Difficulty.MID, new BigDecimal("4.6"), 80, null),
                        candidate(course3.getId(), "C", new BigDecimal("8.0"), Difficulty.MID, new BigDecimal("4.5"), 60, null)
                ));
        given(courseThemeRepository.findAllByCourse_IdIn(List.of(course1.getId(), course2.getId(), course3.getId())))
                .willReturn(List.of(
                        CourseTheme.of(course1, coast),
                        CourseTheme.of(course2, coast)
                ));
        given(courseRecommendationReranker.rerank(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList()))
                .willReturn(List.of(
                        new RerankedRecommendation(
                                UUID.randomUUID(),
                                new RagScoreBreakdown(18.0, 24.0, 18.0, 12.0, 9.0, 9.0),
                                90.0,
                                "invalid course"
                        ),
                        new RerankedRecommendation(
                                course2.getId(),
                                new RagScoreBreakdown(18.0, 24.0, 18.0, 12.0, 9.0, 9.0),
                                101.0,
                                "invalid rag score"
                        ),
                        new RerankedRecommendation(
                                course3.getId(),
                                new RagScoreBreakdown(15.0, 20.0, 18.0, 12.0, 10.0, 10.0),
                                85.0,
                                "AI reason for C"
                        )
                ));

        RecommendedCoursesResponse response = courseRecommendationService.getRecommendedCourses(userId, null, null);

        assertThat(response.recommendations())
                .extracting(RecommendedCoursesResponse.RecommendedCourseItem::courseName)
                .containsExactly("B", "A", "C");
        RecommendedCoursesResponse.RecommendedCourseItem rerankedCourse = response.recommendations().get(2);
        assertThat(rerankedCourse.courseName()).isEqualTo("C");
        assertThat(rerankedCourse.ragScore()).isEqualTo(85.0);
        assertThat(rerankedCourse.finalScore()).isEqualTo(60.25);
        assertThat(rerankedCourse.recommendationReason()).isEqualTo("AI reason for C");

        RecommendedCoursesResponse.RecommendedCourseItem invalidScoreCourse = response.recommendations().get(0);
        assertThat(invalidScoreCourse.courseName()).isEqualTo("B");
        assertThat(invalidScoreCourse.ragScore()).isNull();
        assertThat(invalidScoreCourse.finalScore()).isEqualTo(invalidScoreCourse.baseScore());
    }

    private static User user(UUID userId, PreferredDistance preferredDistance, PreferredDifficulty preferredDifficulty) {
        User user = User.createKakaoUser("kakao-" + userId);
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "preferredDistance", preferredDistance);
        ReflectionTestUtils.setField(user, "preferredDifficulty", preferredDifficulty);
        return user;
    }

    private static Theme theme(String code) {
        return Theme.create(code, code);
    }

    private static Course course(UUID courseId, String name) {
        User creator = User.createKakaoUser("creator-" + courseId);
        ReflectionTestUtils.setField(creator, "id", UUID.randomUUID());
        var route = GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                new Coordinate(126.5, 33.5),
                new Coordinate(126.51, 33.51)
        });
        route.setSRID(4326);
        var startPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(126.5, 33.5));
        startPoint.setSRID(4326);
        Course course = Course.create(
                creator,
                name,
                null,
                CourseType.RUNNING_COURSE,
                new BigDecimal("5.0"),
                50,
                new BigDecimal("20.0"),
                Difficulty.MID,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                route,
                startPoint,
                null,
                true
        );
        ReflectionTestUtils.setField(course, "id", courseId);
        return course;
    }

    private static CourseRecommendationCandidate candidate(
            UUID courseId,
            String courseName,
            BigDecimal distanceKm,
            Difficulty difficulty,
            BigDecimal averageRating,
            Integer completionCount,
            Double distanceMeters
    ) {
        return candidate(
                courseId,
                courseName,
                CourseType.RUNNING_COURSE,
                distanceKm,
                difficulty,
                averageRating,
                completionCount,
                distanceMeters,
                new BigDecimal("30.0")
        );
    }

    private static CourseRecommendationCandidate candidate(
            UUID courseId,
            String courseName,
            CourseType courseType,
            BigDecimal distanceKm,
            Difficulty difficulty,
            BigDecimal averageRating,
            Integer completionCount,
            Double distanceMeters,
            BigDecimal elevationGainM
    ) {
        return new CourseRecommendationCandidate(
                courseId,
                courseName,
                "Jeju running course description for " + courseName,
                courseType,
                distanceKm,
                difficulty,
                elevationGainM,
                averageRating,
                completionCount,
                distanceMeters
        );
    }
}
