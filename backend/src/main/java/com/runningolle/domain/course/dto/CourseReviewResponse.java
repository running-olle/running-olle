package com.runningolle.domain.course.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CourseReviewResponse(
        UUID reviewId,
        UUID userId,
        String nickname,
        String profileImageUrl,
        UUID runningRecordId,
        BigDecimal rating,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean isMine
) {
}
