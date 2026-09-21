package com.runningolle.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthCookieServiceTest {

    @Test
    void createsHttpOnlyStrictCookie() {
        AuthCookieService service = new AuthCookieService(true, 86_400_000);
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.add(response, "signed-token");

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                .contains("runningOlleAccessToken=signed-token")
                .contains("Path=/")
                .contains("Max-Age=86400")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Strict");
    }

    @Test
    void clearsAuthenticationCookie() {
        AuthCookieService service = new AuthCookieService(true, 86_400_000);
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.clear(response);

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                .contains("runningOlleAccessToken=")
                .contains("Max-Age=0");
    }

    @Test
    void jwtProviderReadsAuthenticationCookie() {
        JwtTokenProvider provider = new JwtTokenProvider(
                "01234567890123456789012345678901",
                86_400_000
        );
        String token = provider.createToken("user-id", java.util.Map.of("roles", java.util.List.of("ROLE_USER")));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthCookieService.COOKIE_NAME, token));

        assertThat(provider.resolveToken(request)).isEqualTo(token);
        assertThat(provider.resolveTokenFromCookieHeader(
                "other=value; " + AuthCookieService.COOKIE_NAME + "=" + token
        )).isEqualTo(token);
    }
}
