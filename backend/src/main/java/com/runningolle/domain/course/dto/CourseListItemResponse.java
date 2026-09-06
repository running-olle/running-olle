package com.runningolle.domain.course.dto;

import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CourseListItemResponse(
        UUID id,
        String name,
        String description,
        CourseType courseType,
        BigDecimal distanceKm,
        Integer estimatedDurationMinutes,
        BigDecimal elevationGainM,
        Difficulty difficulty,
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
        List<String> waypointNames,
        List<RouteCoordinateResponse> previewRouteCoordinates,
        List<CourseWaypointResponse> waypoints
) {
    public static CourseListItemResponse from(
            Course course,
            boolean createdByMe,
            boolean bookmarkedByMe,
            UUID bookmarkId,
            List<CourseWaypointResponse> waypoints
    ) {
        return new CourseListItemResponse(
                course.getId(),
                course.getName(),
                course.getDescription(),
                course.getCourseType(),
                course.getDistanceKm(),
                course.getEstimatedDurationMinutes(),
                course.getElevationGainM(),
                course.getDifficulty(),
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
                waypoints.stream().map(CourseWaypointResponse::name).toList(),
                RouteCoordinateResponse.preview(course.getRoute(), 80),
                waypoints
        );
    }
}
