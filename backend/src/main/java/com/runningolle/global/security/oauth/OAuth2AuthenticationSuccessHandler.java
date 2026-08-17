package com.runningolle.global.security.oauth;

import com.runningolle.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String userId = oAuth2User.getAttribute("localUserId");
        boolean onboardingCompleted = Boolean.TRUE.equals(oAuth2User.getAttribute("onboardingCompleted"));
        String accessToken = jwtTokenProvider.createToken(userId, java.util.Map.of(
                "provider", "kakao",
                "roles", java.util.List.of("ROLE_USER"),
                "onboardingCompleted", onboardingCompleted
        ));

        response.sendRedirect(frontendUrl + "/oauth/callback#access_token=" + accessToken
                + "&onboarding_completed=" + onboardingCompleted);

        clearAuthenticationAttributes(request);
    }
}
