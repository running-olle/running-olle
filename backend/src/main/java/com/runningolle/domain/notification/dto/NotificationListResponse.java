package com.runningolle.domain.notification.dto;

import java.util.List;

public record NotificationListResponse(List<NotificationResponse> notifications, long unreadCount) {
}
