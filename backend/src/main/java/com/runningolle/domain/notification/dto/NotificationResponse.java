package com.runningolle.domain.notification.dto;

import com.runningolle.domain.notification.enums.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        String actionUrl,
        boolean read,
        LocalDateTime createdAt
) {
}
