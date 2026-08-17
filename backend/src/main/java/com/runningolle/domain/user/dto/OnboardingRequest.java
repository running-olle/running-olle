package com.runningolle.domain.user.dto;

import com.runningolle.domain.user.enums.PreferredDifficulty;
import com.runningolle.domain.user.enums.PreferredDistance;
import com.runningolle.domain.user.enums.UserTypeCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record OnboardingRequest(
        @NotBlank @Size(max = 100) String nickname,
        @Size(max = 4_200_000) String profileImageUrl,
        @Size(max = 100) String bio,
        @NotEmpty Set<UserTypeCode> userTypes,
        @NotNull PreferredDistance preferredDistance,
        @NotNull PreferredDifficulty preferredDifficulty,
        @NotNull @Valid Terms terms,
        @NotNull @Valid Notifications notifications
) {
    public record Terms(
            @AssertTrue boolean service,
            @AssertTrue boolean privacy,
            @AssertTrue boolean location,
            boolean marketing
    ) {}

    public record Notifications(
            boolean recommendedCourse,
            boolean weather,
            boolean meetupInvite,
            boolean commentLike
    ) {}
}
