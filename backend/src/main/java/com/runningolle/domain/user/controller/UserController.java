package com.runningolle.domain.user.controller;

import com.runningolle.domain.user.dto.CurrentUserResponse;
import com.runningolle.domain.user.dto.OnboardingRequest;
import com.runningolle.domain.user.service.UserService;
import com.runningolle.domain.mypage.dto.MyPageDtos;
import com.runningolle.domain.mypage.service.MyPageService;
import com.runningolle.domain.community.dto.ImageUploadResponse;
import com.runningolle.domain.community.storage.FileStorageService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final MyPageService myPageService;
    private final FileStorageService fileStorageService;

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

    @GetMapping("/me/profile")
    public MyPageDtos.Profile getProfile(Authentication authentication) {
        return myPageService.profile(UUID.fromString(authentication.getName()));
    }

    @PutMapping("/me/profile")
    public MyPageDtos.Profile updateProfile(Authentication authentication, @Valid @RequestBody MyPageDtos.UpdateProfileRequest request) {
        return myPageService.updateProfile(UUID.fromString(authentication.getName()), request);
    }

    @PostMapping("/me/profile/image")
    public ImageUploadResponse uploadProfileImage(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일을 선택해주세요.");
        }
        try {
            return new ImageUploadResponse(fileStorageService.store(List.of(file)));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "프로필 사진 업로드에 실패했습니다.");
        }
    }

    @GetMapping("/me/notifications")
    public MyPageDtos.NotificationSettings getNotifications(Authentication authentication) {
        return myPageService.notifications(UUID.fromString(authentication.getName()));
    }

    @PutMapping("/me/notifications")
    public MyPageDtos.NotificationSettings updateNotifications(Authentication authentication, @RequestBody MyPageDtos.NotificationSettings request) {
        return myPageService.updateNotifications(UUID.fromString(authentication.getName()), request);
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
