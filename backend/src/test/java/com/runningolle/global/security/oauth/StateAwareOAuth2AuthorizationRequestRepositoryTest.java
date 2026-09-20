package com.runningolle.global.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

class StateAwareOAuth2AuthorizationRequestRepositoryTest {

    private final StateAwareOAuth2AuthorizationRequestRepository repository =
            new StateAwareOAuth2AuthorizationRequestRepository();

    @Test
    void keepsConcurrentAuthorizationRequestsSeparatedByState() {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthorizationRequest first = authorizationRequest("first-state");
        OAuth2AuthorizationRequest second = authorizationRequest("second-state");

        repository.saveAuthorizationRequest(first, request(session, null), response);
        repository.saveAuthorizationRequest(second, request(session, null), response);

        assertThat(repository.loadAuthorizationRequest(request(session, "first-state"))).isEqualTo(first);
        assertThat(repository.loadAuthorizationRequest(request(session, "second-state"))).isEqualTo(second);
    }

    @Test
    void removingOneRequestDoesNotRemoveAnotherPendingRequest() {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthorizationRequest first = authorizationRequest("first-state");
        OAuth2AuthorizationRequest second = authorizationRequest("second-state");
        repository.saveAuthorizationRequest(first, request(session, null), response);
        repository.saveAuthorizationRequest(second, request(session, null), response);

        assertThat(repository.removeAuthorizationRequest(request(session, "first-state"), response)).isEqualTo(first);
        assertThat(repository.loadAuthorizationRequest(request(session, "first-state"))).isNull();
        assertThat(repository.loadAuthorizationRequest(request(session, "second-state"))).isEqualTo(second);
    }

    private MockHttpServletRequest request(MockHttpSession session, String state) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        if (state != null) {
            request.setParameter("state", state);
        }
        return request;
    }

    private OAuth2AuthorizationRequest authorizationRequest(String state) {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .clientId("client-id")
                .redirectUri("https://running-olle.site/api/login/oauth2/code/kakao")
                .state(state)
                .build();
    }
}
