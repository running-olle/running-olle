package com.runningolle.domain.tourism.dto;

import java.time.LocalDateTime;

public record TourismEventSyncResponse(
        String areaCode,
        int fetchedCount,
        int createdCount,
        int updatedCount,
        int skippedCount,
        int failedCount,
        LocalDateTime syncedAt
) {
}
