package com.runningolle.domain.user.repository;

import com.runningolle.domain.user.entity.UserNotificationSetting;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationSettingRepository extends JpaRepository<UserNotificationSetting, UUID> {
    Optional<UserNotificationSetting> findByUserId(UUID userId);
}
