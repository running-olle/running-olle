package com.runningolle.domain.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.runningolle.domain.user.dto.OnboardingRequest;
import com.runningolle.domain.user.entity.Theme;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.entity.UserNotificationSetting;
import com.runningolle.domain.user.entity.UserType;
import com.runningolle.domain.user.enums.PreferredDifficulty;
import com.runningolle.domain.user.enums.PreferredDistance;
import com.runningolle.domain.user.enums.UserTypeCode;
import com.runningolle.domain.user.repository.ThemeRepository;
import com.runningolle.domain.user.repository.UserNotificationSettingRepository;
import com.runningolle.domain.user.repository.UserRepository;
import com.runningolle.domain.user.repository.UserThemeRepository;
import com.runningolle.domain.user.repository.UserTypeRepository;
import com.runningolle.domain.user.repository.UserUserTypeRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ThemeRepository themeRepository;

    @Mock
    private UserTypeRepository userTypeRepository;

    @Mock
    private UserUserTypeRepository userUserTypeRepository;

    @Mock
    private UserThemeRepository userThemeRepository;

    @Mock
    private UserNotificationSettingRepository userNotificationSettingRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                themeRepository,
                userTypeRepository,
                userUserTypeRepository,
                userThemeRepository,
                userNotificationSettingRepository
        );
    }

    @Test
    void savesSelectedThemesDuringOnboarding() {
        UUID userId = UUID.randomUUID();
        UUID themeId = UUID.randomUUID();
        User user = User.createKakaoUser("kakao-1");
        ReflectionTestUtils.setField(user, "id", userId);
        Theme theme = Theme.create("COAST", "Coast");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("runner")).willReturn(false);
        given(userTypeRepository.findByCode("ACTIVE_RUNNER"))
                .willReturn(Optional.of(UserType.of("ACTIVE_RUNNER", "Active runner")));
        given(themeRepository.findAllById(List.of(themeId))).willReturn(List.of(theme));
        given(userNotificationSettingRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(userNotificationSettingRepository.save(org.mockito.ArgumentMatchers.any(UserNotificationSetting.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        userService.completeOnboarding(userId, new OnboardingRequest(
                "runner",
                null,
                null,
                Set.of(UserTypeCode.ACTIVE_RUNNER),
                PreferredDistance.UNDER_3KM,
                PreferredDifficulty.EASY,
                List.of(themeId),
                new OnboardingRequest.Terms(true, true, true, false),
                new OnboardingRequest.Notifications(true, true, true, false)
        ));

        verify(userThemeRepository).deleteAllByUserId(userId);
        verify(userThemeRepository).saveAll(anyList());
    }

    @Test
    void throwsWhenSelectedThemeDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID themeId = UUID.randomUUID();
        User user = User.createKakaoUser("kakao-1");
        ReflectionTestUtils.setField(user, "id", userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("runner")).willReturn(false);
        given(userTypeRepository.findByCode("ACTIVE_RUNNER"))
                .willReturn(Optional.of(UserType.of("ACTIVE_RUNNER", "Active runner")));
        given(themeRepository.findAllById(List.of(themeId))).willReturn(List.of());

        assertThatThrownBy(() -> userService.completeOnboarding(userId, new OnboardingRequest(
                "runner",
                null,
                null,
                Set.of(UserTypeCode.ACTIVE_RUNNER),
                PreferredDistance.UNDER_3KM,
                PreferredDifficulty.EASY,
                List.of(themeId),
                new OnboardingRequest.Terms(true, true, true, false),
                new OnboardingRequest.Notifications(true, true, true, false)
        )))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("themes");
    }
}
