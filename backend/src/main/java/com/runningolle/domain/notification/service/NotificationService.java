package com.runningolle.domain.notification.service;

import com.runningolle.domain.notification.dto.NotificationListResponse;
import com.runningolle.domain.notification.dto.NotificationResponse;
import com.runningolle.domain.notification.entity.UserNotification;
import com.runningolle.domain.notification.enums.NotificationType;
import com.runningolle.domain.notification.repository.UserNotificationRepository;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.repository.UserNotificationSettingRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserNotificationRepository notificationRepository;
    private final UserNotificationSettingRepository settingRepository;

    @Transactional
    public void createSocial(User recipient, NotificationType type, String title, String message, String sourceKey) {
        boolean enabled = settingRepository.findByUserId(recipient.getId())
                .map(setting -> Boolean.TRUE.equals(setting.getCommentLike()))
                .orElse(true);
        createIfEnabled(recipient, type, title, message, sourceKey, "/community?tab=feed", enabled);
    }

    @Transactional
    public void createMeetup(User recipient, NotificationType type, String title, String message, String sourceKey) {
        boolean enabled = settingRepository.findByUserId(recipient.getId())
                .map(setting -> Boolean.TRUE.equals(setting.getMeetupInvite()))
                .orElse(true);
        createIfEnabled(recipient, type, title, message, sourceKey, "/community?tab=meetup", enabled);
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(UUID userId) {
        var notifications = notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
        return new NotificationListResponse(notifications, notificationRepository.countByUserIdAndReadAtIsNull(userId));
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."))
                .markRead();
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.findByUserIdAndReadAtIsNull(userId)
                .forEach(UserNotification::markRead);
    }

    @Transactional
    public void clear(UUID userId) {
        notificationRepository.deleteByUserId(userId);
    }

    private void createIfEnabled(
            User recipient,
            NotificationType type,
            String title,
            String message,
            String sourceKey,
            String actionUrl,
            boolean enabled
    ) {
        if (!enabled || notificationRepository.existsBySourceKey(sourceKey)) {
            return;
        }
        notificationRepository.save(UserNotification.create(
                recipient, type, title, message, actionUrl, sourceKey
        ));
    }

    private NotificationResponse toResponse(UserNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getActionUrl(),
                notification.getReadAt() != null,
                notification.getCreatedAt()
        );
    }
}
