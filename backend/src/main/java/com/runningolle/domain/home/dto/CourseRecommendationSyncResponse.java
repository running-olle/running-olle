package com.runningolle.domain.home.dto;

import java.time.LocalDateTime;

public record CourseRecommendationSyncResponse(
        int targetCourseCount,
        int documentCreatedCount,
        int documentUpdatedCount,
        int documentDeletedCount,
        int documentSkippedCount,
        int embeddingSyncedCount,
        int embeddingDeletedCount,
        int embeddingSkippedCount,
        int embeddingFailedCount,
        LocalDateTime syncedAt
) {
}
