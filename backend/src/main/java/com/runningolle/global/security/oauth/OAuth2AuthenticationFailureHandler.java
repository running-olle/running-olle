package com.runningolle.global.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Pattern KAKAO_ERROR_CODE = Pattern.compile("\\bKOE\\d{3}\\b");

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String errorCode = exception instanceof OAuth2AuthenticationException oauthException
                ? oauthException.getError().getErrorCode()
                : "oauth_login_failed";
        String providerCode = findKakaoErrorCode(exception);
        String publicErrorCode = toPublicErrorCode(errorCode, providerCode, exception);
        String traceId = UUID.randomUUID().toString().substring(0, 8);

        // Do not log the exception message or token response body. Kakao's KOE320
        // response can contain the one-time authorization code.
        log.warn(
                "OAuth login failed: traceId={}, errorCode={}, providerCode={}, exceptionType={}",
                traceId,
                errorCode,
                providerCode == null ? "unknown" : providerCode,
                exception.getClass().getSimpleName()
        );

        String encodedErrorCode = URLEncoder.encode(publicErrorCode, StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(
                request,
                response,
                frontendUrl + "/login?oauth_error=" + encodedErrorCode + "&oauth_trace=" + traceId
        );
    }

    static String findKakaoErrorCode(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof OAuth2AuthenticationException oauthException) {
                String code = findKakaoErrorCode(oauthException.getError().getDescription());
                if (code != null) {
                    return code;
                }
            }

            String code = findKakaoErrorCode(current.getMessage());
            if (code != null) {
                return code;
            }
            current = current.getCause();
        }
        return null;
    }

    private static String findKakaoErrorCode(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = KAKAO_ERROR_CODE.matcher(value.toUpperCase(Locale.ROOT));
        return matcher.find() ? matcher.group() : null;
    }

    static String toPublicErrorCode(String errorCode, String providerCode, Throwable throwable) {
        if ("KOE237".equals(providerCode)) {
            return "oauth_rate_limited";
        }
        if ("KOE320".equals(providerCode)) {
            return "authorization_code_invalid";
        }
        if ("KOE003".equals(providerCode)) {
            return "oauth_provider_unavailable";
        }
        if (providerCode != null && switch (providerCode) {
            case "KOE010", "KOE101", "KOE114", "KOE127", "KOE303" -> true;
            default -> false;
        }) {
            return "oauth_configuration_error";
        }
        if ("invalid_token_response".equals(errorCode)) {
            return hasConnectionFailure(throwable)
                    ? "oauth_provider_unavailable"
                    : "oauth_token_exchange_failed";
        }
        return errorCode;
    }

    private static boolean hasConnectionFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String type = current.getClass().getSimpleName().toLowerCase(Locale.ROOT);
            if (type.contains("timeout") || type.contains("connect") || type.contains("resourceaccess")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
