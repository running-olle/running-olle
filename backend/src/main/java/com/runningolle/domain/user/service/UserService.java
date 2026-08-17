package com.runningolle.domain.user.service;

import com.runningolle.domain.user.dto.OnboardingRequest;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.entity.UserNotificationSetting;
import com.runningolle.domain.user.entity.UserType;
import com.runningolle.domain.user.entity.UserUserType;
import com.runningolle.domain.user.enums.AccountStatus;
import com.runningolle.domain.user.enums.UserTypeCode;
import com.runningolle.domain.user.repository.UserNotificationSettingRepository;
import com.runningolle.domain.user.repository.UserRepository;
import com.runningolle.domain.user.repository.UserTypeRepository;
import com.runningolle.domain.user.repository.UserUserTypeRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserTypeRepository userTypeRepository;
    private final UserUserTypeRepository userUserTypeRepository;
    private final UserNotificationSettingRepository notificationSettingRepository;

    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname) {
        return nickname != null && !nickname.isBlank() && !userRepository.existsByNickname(nickname.trim());
    }

    @Transactional(readOnly = true)
    public boolean isOnboardingCompleted(UUID userId) {
        return Boolean.TRUE.equals(getActiveUser(userId).getOnboardingCompleted());
    }

    @Transactional
    public void completeOnboarding(UUID userId, OnboardingRequest request) {
        User user = getActiveUser(userId);
        String nickname = request.nickname().trim();
        if (userRepository.existsByNickname(nickname)
                && !nickname.equals(user.getNickname())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }

        user.completeOnboarding(
                nickname,
                request.profileImageUrl(),
                request.bio() == null ? null : request.bio().trim(),
                request.preferredDistance(),
                request.preferredDifficulty(),
                request.terms().service(),
                request.terms().privacy(),
                request.terms().location(),
                request.terms().marketing()
        );

        userUserTypeRepository.deleteAllByUserId(userId);
        for (UserTypeCode code : request.userTypes()) {
            UserType type = userTypeRepository.findByCode(code.name())
                    .orElseGet(() -> userTypeRepository.save(UserType.of(code.name(), code.getDisplayName())));
            userUserTypeRepository.save(UserUserType.of(user, type));
        }

        notificationSettingRepository.findByUserId(userId)
                .ifPresent(notificationSettingRepository::delete);
        notificationSettingRepository.flush();
        notificationSettingRepository.save(UserNotificationSetting.create(
                user,
                request.terms().marketing(),
                request.notifications().recommendedCourse(),
                request.notifications().weather(),
                request.notifications().meetupInvite(),
                request.notifications().commentLike()
        ));
    }

    @Transactional
    public void withdraw(UUID userId) {
        getActiveUser(userId).withdraw();
    }

    private User getActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "활성 계정이 아닙니다.");
        }
        return user;
    }
}
