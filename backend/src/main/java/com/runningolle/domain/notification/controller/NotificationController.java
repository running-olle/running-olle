package com.runningolle.domain.notification.controller;

import com.runningolle.domain.notification.dto.NotificationListResponse;
import com.runningolle.domain.notification.service.NotificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public NotificationListResponse getNotifications(Authentication authentication) {
        return notificationService.getNotifications(UUID.fromString(authentication.getName()));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(Authentication authentication, @PathVariable UUID notificationId) {
        notificationService.markRead(UUID.fromString(authentication.getName()), notificationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        notificationService.markAllRead(UUID.fromString(authentication.getName()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(Authentication authentication) {
        notificationService.clear(UUID.fromString(authentication.getName()));
        return ResponseEntity.noContent().build();
    }
}
