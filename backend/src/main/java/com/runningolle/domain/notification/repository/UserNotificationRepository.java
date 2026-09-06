package com.runningolle.domain.notification.repository;

import com.runningolle.domain.notification.entity.UserNotification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {
    List<UserNotification> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);
    List<UserNotification> findByUserIdAndReadAtIsNull(UUID userId);
    Optional<UserNotification> findByIdAndUserId(UUID id, UUID userId);
    boolean existsBySourceKey(String sourceKey);
    long countByUserIdAndReadAtIsNull(UUID userId);
    void deleteByUserId(UUID userId);
}
