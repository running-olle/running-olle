package com.runningolle.global.security.jwt;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieService {

    public static final String COOKIE_NAME = "runningOlleAccessToken";

    private final boolean secure;
    private final Duration maxAge;

    public AuthCookieService(
            @Value("${app.auth-cookie-secure:false}") boolean secure,
            @Value("${jwt.expiration}") long expirationMillis
    ) {
        this.secure = secure;
        this.maxAge = Duration.ofMillis(expirationMillis);
    }

    public void add(HttpServletResponse response, String token) {
        addCookie(response, token, maxAge);
    }

    public void clear(HttpServletResponse response) {
        addCookie(response, "", Duration.ZERO);
    }

    private void addCookie(HttpServletResponse response, String value, Duration age) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(age)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
