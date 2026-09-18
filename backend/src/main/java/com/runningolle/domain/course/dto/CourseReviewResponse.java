package com.runningolle.domain.course.dto;

import com.runningolle.domain.course.entity.CourseReview;
import java.time.LocalDateTime;
import java.util.UUID;

public record CourseReviewResponse(
        UUID id,
        UUID userId,
        String userNickname,
        UUID runningRecordId,
        int rating,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean authoredByMe
) {
    public static CourseReviewResponse from(CourseReview review, UUID currentUserId) {
        return new CourseReviewResponse(
                review.getId(),
                review.getUser().getId(),
                review.getUser().getNickname(),
                review.getRunningRecord().getId(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.getUser().getId().equals(currentUserId)
        );
    }
}
