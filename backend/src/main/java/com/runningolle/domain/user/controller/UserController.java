package com.runningolle.domain.user.controller;

import com.runningolle.domain.user.dto.CurrentUserResponse;
import com.runningolle.domain.user.dto.OnboardingRequest;
import com.runningolle.domain.user.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/nickname-availability")
    public Map<String, Boolean> nicknameAvailability(@RequestParam String nickname) {
        return Map.of("available", userService.isNicknameAvailable(nickname));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> getCurrentUser(Authentication authentication) {
        boolean onboardingCompleted = userService.isOnboardingCompleted(
                UUID.fromString(authentication.getName())
        );
        return ResponseEntity.ok(new CurrentUserResponse(onboardingCompleted));
    }

    @PostMapping("/me/onboarding")
    public ResponseEntity<Void> completeOnboarding(
            Authentication authentication,
            @Valid @RequestBody OnboardingRequest request
    ) {
        userService.completeOnboarding(UUID.fromString(authentication.getName()), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(Authentication authentication) {
        userService.withdraw(UUID.fromString(authentication.getName()));
        return ResponseEntity.noContent().build();
    }
}
