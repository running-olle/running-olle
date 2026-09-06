package com.runningolle.domain.course.dto;

import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CourseDetailResponse(
        UUID id,
        String name,
        String description,
        CourseType courseType,
        BigDecimal distanceKm,
        Integer estimatedDurationMinutes,
        BigDecimal elevationGainM,
        Difficulty difficulty,
        BigDecimal surfaceAsphaltPct,
        BigDecimal surfaceDirtPct,
        BigDecimal surfaceStairsPct,
        String thumbnailImageUrl,
        Boolean isPublic,
        BigDecimal ratingAvg,
        Integer completionCount,
        UUID creatorId,
        String creatorNickname,
        boolean createdByMe,
        boolean bookmarkedByMe,
        UUID bookmarkId,
        LocalDateTime createdAt,
        List<RouteCoordinateResponse> routeCoordinates,
        List<CourseWaypointResponse> waypoints
) {

    public static CourseDetailResponse from(
            Course course,
            boolean createdByMe,
            boolean bookmarkedByMe,
            UUID bookmarkId,
            List<CourseWaypointResponse> waypoints
    ) {
        return new CourseDetailResponse(
                course.getId(),
                course.getName(),
                course.getDescription(),
                course.getCourseType(),
                course.getDistanceKm(),
                course.getEstimatedDurationMinutes(),
                course.getElevationGainM(),
                course.getDifficulty(),
                course.getSurfaceAsphaltPct(),
                course.getSurfaceDirtPct(),
                course.getSurfaceStairsPct(),
                course.getThumbnailImageUrl(),
                course.getIsPublic(),
                course.getRatingAvg(),
                course.getCompletionCount(),
                course.getCreator().getId(),
                course.getCreator().getNickname(),
                createdByMe,
                bookmarkedByMe,
                bookmarkId,
                course.getCreatedAt(),
                RouteCoordinateResponse.from(course.getRoute()),
                waypoints
        );
    }
}
