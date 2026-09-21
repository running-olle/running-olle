package com.runningolle.global.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

class OAuth2AuthenticationFailureHandlerTest {

    private OAuth2AuthenticationFailureHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2AuthenticationFailureHandler();
        ReflectionTestUtils.setField(handler, "frontendUrl", "https://running-olle.site");
    }

    @Test
    void mapsKakaoRateLimitWithoutExposingTokenResponse() throws Exception {
        OAuth2AuthenticationException exception = oauthException(
                "invalid_token_response",
                "400 Bad Request: {\"error_code\":\"KOE237\",\"error\":\"invalid_request\"}"
        );
        MockHttpServletResponse response = handle(exception);

        assertThat(response.getRedirectedUrl())
                .startsWith("https://running-olle.site/login?oauth_error=oauth_rate_limited&oauth_trace=");
        assertThat(response.getRedirectedUrl()).doesNotContain("KOE237");
    }

    @Test
    void mapsExpiredOrReusedAuthorizationCode() throws Exception {
        OAuth2AuthenticationException exception = oauthException(
                "invalid_token_response",
                "KOE320 authorization code not found for code=sensitive-one-time-code"
        );
        MockHttpServletResponse response = handle(exception);

        assertThat(response.getRedirectedUrl())
                .startsWith("https://running-olle.site/login?oauth_error=authorization_code_invalid&oauth_trace=");
        assertThat(response.getRedirectedUrl()).doesNotContain("sensitive-one-time-code");
    }

    @Test
    void keepsAuthorizationRequestNotFoundAsRecoverableError() throws Exception {
        MockHttpServletResponse response = handle(oauthException("authorization_request_not_found", null));

        assertThat(response.getRedirectedUrl())
                .startsWith("https://running-olle.site/login?oauth_error=authorization_request_not_found&oauth_trace=");
    }

    @Test
    void mapsUnknownTokenResponseToFreshLoginError() throws Exception {
        MockHttpServletResponse response = handle(oauthException("invalid_token_response", "unexpected response"));

        assertThat(response.getRedirectedUrl())
                .startsWith("https://running-olle.site/login?oauth_error=oauth_token_exchange_failed&oauth_trace=");
    }

    private MockHttpServletResponse handle(OAuth2AuthenticationException exception) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationFailure(new MockHttpServletRequest(), response, exception);
        return response;
    }

    private OAuth2AuthenticationException oauthException(String code, String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(code, description, null));
    }
}
