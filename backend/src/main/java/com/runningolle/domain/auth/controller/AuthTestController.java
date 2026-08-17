package com.runningolle.domain.auth.controller;

import com.runningolle.domain.auth.dto.TestTokenRequest;
import com.runningolle.domain.auth.dto.TestTokenResponse;
import com.runningolle.domain.user.repository.UserRepository;
import com.runningolle.global.security.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthTestController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @PostMapping("/test-token")
    public TestTokenResponse issueTestToken(@Valid @RequestBody TestTokenRequest request) {
        userRepository.findByKakaoId(request.kakaoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        Map<String, Object> claims = new HashMap<>();
        claims.put("provider", "kakao");

        String accessToken = jwtTokenProvider.createToken("kakao:" + request.kakaoId(), claims);

        return new TestTokenResponse(
                "Bearer",
                accessToken,
                "kakao",
                request.kakaoId()
        );
    }
}
