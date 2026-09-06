package com.runningolle.domain.home.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.home.service.CourseRecommendationReranker.BaseRecommendationCandidate;
import com.runningolle.domain.home.service.CourseRecommendationReranker.RagScoreBreakdown;
import com.runningolle.domain.home.service.CourseRecommendationReranker.RerankedRecommendation;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CourseRecommendationRerankValidatorTest {

    private final CourseRecommendationRerankValidator validator = new CourseRecommendationRerankValidator();

    @Test
    void keepsOnlyRecommendationsForProvidedBaseCandidates() {
        BaseRecommendationCandidate candidate = baseCandidate("A");

        Map<UUID, RerankedRecommendation> validated = validator.validate(
                List.of(candidate),
                List.of(validRecommendation(candidate.courseId(), 76.0), validRecommendation(UUID.randomUUID(), 70.0))
        );

        assertThat(validated).hasSize(1);
        assertThat(validated).containsOnlyKeys(candidate.courseId());
    }

    @Test
    void returnsEmptyWhenRerankedRecommendationsAreNullOrEmpty() {
        BaseRecommendationCandidate candidate = baseCandidate("A");

        assertThat(validator.validate(List.of(candidate), null)).isEmpty();
        assertThat(validator.validate(List.of(candidate), List.of())).isEmpty();
    }

    @Test
    void ignoresNullItemsAndItemsWithoutCourseId() {
        BaseRecommendationCandidate candidate = baseCandidate("A");

        Map<UUID, RerankedRecommendation> validated = validator.validate(
                List.of(candidate),
                java.util.Arrays.asList(
                        null,
                        new RerankedRecommendation(null, new RagScoreBreakdown(20.0, 20.0, 15.0, 10.0, 5.0, 6.0), 76.0, "missing id"),
                        validRecommendation(candidate.courseId(), 76.0)
                )
        );

        assertThat(validated).containsOnlyKeys(candidate.courseId());
    }

    @Test
    void ignoresRecommendationsWithInvalidScoreBreakdownOrTotal() {
        BaseRecommendationCandidate candidate = baseCandidate("A");

        Map<UUID, RerankedRecommendation> validated = validator.validate(
                List.of(candidate),
                List.of(
                        new RerankedRecommendation(
                                candidate.courseId(),
                                new RagScoreBreakdown(21.0, 20.0, 15.0, 10.0, 5.0, 5.0),
                                76.0,
                                "invalid userTypeFit"
                        ),
                        new RerankedRecommendation(
                                candidate.courseId(),
                                new RagScoreBreakdown(20.0, 20.0, 15.0, 10.0, 5.0, 5.0),
                                70.0,
                                "mismatched total"
                        )
                )
        );

        assertThat(validated).isEmpty();
    }

    @Test
    void keepsFirstValidRecommendationWhenDuplicatesExist() {
        BaseRecommendationCandidate candidate = baseCandidate("A");
        RerankedRecommendation first = validRecommendation(candidate.courseId(), 76.0);
        RerankedRecommendation second = new RerankedRecommendation(
                candidate.courseId(),
                new RagScoreBreakdown(20.0, 20.0, 15.0, 10.0, 6.0, 6.0),
                77.0,
                "second"
        );

        Map<UUID, RerankedRecommendation> validated = validator.validate(
                List.of(candidate),
                List.of(first, second)
        );

        assertThat(validated).containsEntry(candidate.courseId(), first);
    }

    private BaseRecommendationCandidate baseCandidate(String courseName) {
        return new BaseRecommendationCandidate(
                UUID.randomUUID(),
                courseName,
                "Description for " + courseName,
                CourseType.RUNNING_COURSE,
                new BigDecimal("6.0"),
                Difficulty.MID,
                new BigDecimal("4.5"),
                50,
                List.of("COAST"),
                2.5,
                80.0
        );
    }

    private RerankedRecommendation validRecommendation(UUID courseId, double ragScore) {
        return new RerankedRecommendation(
                courseId,
                new RagScoreBreakdown(20.0, 20.0, 15.0, 10.0, 5.0, 6.0),
                ragScore,
                "valid"
        );
    }
}
