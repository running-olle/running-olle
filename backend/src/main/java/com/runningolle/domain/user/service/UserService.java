package com.runningolle.domain.user.service;

import com.runningolle.domain.user.dto.OnboardingRequest;
import com.runningolle.domain.user.entity.Theme;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.entity.UserNotificationSetting;
import com.runningolle.domain.user.entity.UserTheme;
import com.runningolle.domain.user.entity.UserType;
import com.runningolle.domain.user.entity.UserUserType;
import com.runningolle.domain.user.enums.AccountStatus;
import com.runningolle.domain.user.enums.UserTypeCode;
import com.runningolle.domain.user.repository.ThemeRepository;
import com.runningolle.domain.user.repository.UserNotificationSettingRepository;
import com.runningolle.domain.user.repository.UserRepository;
import com.runningolle.domain.user.repository.UserThemeRepository;
import com.runningolle.domain.user.repository.UserTypeRepository;
import com.runningolle.domain.user.repository.UserUserTypeRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
    private final ThemeRepository themeRepository;
    private final UserTypeRepository userTypeRepository;
    private final UserUserTypeRepository userUserTypeRepository;
    private final UserThemeRepository userThemeRepository;
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
        syncUserThemes(user, request.themeIds());

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

    private void syncUserThemes(User user, List<UUID> themeIds) {
        userThemeRepository.deleteAllByUserId(user.getId());
        List<UUID> distinctThemeIds = distinctIds(themeIds);
        if (distinctThemeIds.isEmpty()) {
            return;
        }

        List<Theme> themes = themeRepository.findAllById(distinctThemeIds);
        if (themes.size() != distinctThemeIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Some selected themes do not exist.");
        }

        List<UserTheme> userThemes = new ArrayList<>(themes.size());
        for (Theme theme : themes) {
            userThemes.add(UserTheme.of(user, theme));
        }
        userThemeRepository.saveAll(userThemes);
    }

    private List<UUID> distinctIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<UUID> distinctIds = new LinkedHashSet<>();
        for (UUID id : ids) {
            if (id == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Theme id is required.");
            }
            distinctIds.add(id);
        }
        return new ArrayList<>(distinctIds);
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
