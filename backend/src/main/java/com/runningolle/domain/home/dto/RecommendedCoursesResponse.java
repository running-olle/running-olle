package com.runningolle.domain.home.dto;

import com.runningolle.domain.course.enums.Difficulty;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RecommendedCoursesResponse(
        List<RecommendedCourseItem> recommendations
) {

    public record RecommendedCourseItem(
            UUID courseId,
            String courseName,
            BigDecimal distanceKm,
            Difficulty difficulty,
            List<String> themes,
            BigDecimal averageRating,
            Double distanceFromUserKm,
            double baseScore,
            Double ragScore,
            double finalScore,
            String recommendationReason
    ) {
    }
}
