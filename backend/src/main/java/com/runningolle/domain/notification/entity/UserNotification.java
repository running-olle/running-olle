package com.runningolle.domain.notification.entity;

import com.runningolle.domain.notification.enums.NotificationType;
import com.runningolle.domain.user.entity.User;
import com.runningolle.global.entity.BaseCreatedAtEntity;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(
        name = "user_notifications",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_notifications_source_key", columnNames = "source_key")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Access(AccessType.FIELD)
public class UserNotification extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "source_key", nullable = false, length = 220)
    private String sourceKey;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public static UserNotification create(
            User user,
            NotificationType type,
            String title,
            String message,
            String actionUrl,
            String sourceKey
    ) {
        UserNotification notification = new UserNotification();
        notification.user = user;
        notification.type = type;
        notification.title = title;
        notification.message = message;
        notification.actionUrl = actionUrl;
        notification.sourceKey = sourceKey;
        return notification;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }
}
