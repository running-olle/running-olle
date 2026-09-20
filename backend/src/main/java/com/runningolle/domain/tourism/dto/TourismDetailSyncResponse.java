package com.runningolle.domain.tourism.dto;

import java.time.LocalDateTime;

public record TourismDetailSyncResponse(
        int selectedCount,
        int processedCount,
        int completedCount,
        int failedCount,
        int deferredCount,
        LocalDateTime processedAt
) {
}
