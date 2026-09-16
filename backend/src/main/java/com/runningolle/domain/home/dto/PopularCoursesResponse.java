package com.runningolle.domain.home.dto;

import com.runningolle.domain.course.enums.Difficulty;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PopularCoursesResponse(
        List<PopularCourseItem> courses
) {

    public record PopularCourseItem(
            UUID courseId,
            int rank,
            String courseName,
            BigDecimal distanceKm,
            Difficulty difficulty,
            long participantCount,
            String thumbnailImageUrl
    ) {
    }
}
