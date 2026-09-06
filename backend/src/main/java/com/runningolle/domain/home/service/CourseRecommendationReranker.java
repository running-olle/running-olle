package com.runningolle.domain.home.service;

import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.user.enums.PreferredDifficulty;
import com.runningolle.domain.user.enums.PreferredDistance;
import com.runningolle.domain.user.enums.UserTypeCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Reranks base recommendation candidates that were already selected by structured scoring.
 * Current implementations must not discover new courseIds outside the provided candidate set.
 */
public interface CourseRecommendationReranker {

    List<RerankedRecommendation> rerank(
            RecommendationUserPreference preference,
            List<BaseRecommendationCandidate> candidates
    );

    record RecommendationUserPreference(
            UUID userId,
            PreferredDistance preferredDistance,
            PreferredDifficulty preferredDifficulty,
            Set<UserTypeCode> userTypes,
            Set<String> themeCodes
    ) {
    }

    record BaseRecommendationCandidate(
            UUID courseId,
            String courseName,
            String description,
            CourseType courseType,
            BigDecimal distanceKm,
            Difficulty difficulty,
            BigDecimal averageRating,
            Integer completionCount,
            List<String> themeCodes,
            Double distanceFromUserKm,
            double baseScore
    ) {
    }

    record RerankedRecommendation(
            UUID courseId,
            RagScoreBreakdown scores,
            double ragScore,
            String recommendationReason
    ) {
    }

    record RagScoreBreakdown(
            double userTypeFit,
            double themeFit,
            double reviewSatisfaction,
            double seasonFit,
            double safety,
            double convenience
    ) {
        public double total() {
            return userTypeFit + themeFit + reviewSatisfaction + seasonFit + safety + convenience;
        }
    }
}
