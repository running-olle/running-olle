package com.runningolle.global.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

class StateAwareOAuth2AuthorizationRequestRepositoryTest {

    private StateAwareOAuth2AuthorizationRequestRepository repository;

    @BeforeEach
    void setUp() {
        Map<String, OAuthAuthorizationRequestState> storage = new HashMap<>();
        OAuthAuthorizationRequestStateRepository stateRepository =
                mock(OAuthAuthorizationRequestStateRepository.class);

        when(stateRepository.save(any())).thenAnswer(invocation -> {
            OAuthAuthorizationRequestState saved = invocation.getArgument(0);
            storage.put(saved.getState(), saved);
            return saved;
        });
        when(stateRepository.findById(any())).thenAnswer(invocation ->
                Optional.ofNullable(storage.get(invocation.getArgument(0))));
        when(stateRepository.findByStateForUpdate(any())).thenAnswer(invocation ->
                Optional.ofNullable(storage.get(invocation.getArgument(0))));
        doAnswer(invocation -> {
            storage.remove(invocation.getArgument(0));
            return null;
        }).when(stateRepository).deleteById(any());

        repository = new StateAwareOAuth2AuthorizationRequestRepository(stateRepository);
    }

    @Test
    void keepsConcurrentAuthorizationRequestsSeparatedByState() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthorizationRequest first = authorizationRequest("first-state");
        OAuth2AuthorizationRequest second = authorizationRequest("second-state");

        repository.saveAuthorizationRequest(first, request(null), response);
        repository.saveAuthorizationRequest(second, request(null), response);

        assertThat(repository.loadAuthorizationRequest(request("first-state")).getState()).isEqualTo("first-state");
        assertThat(repository.loadAuthorizationRequest(request("second-state")).getState()).isEqualTo("second-state");
    }

    @Test
    void removingOneRequestDoesNotRemoveAnotherPendingRequest() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthorizationRequest first = authorizationRequest("first-state");
        OAuth2AuthorizationRequest second = authorizationRequest("second-state");
        repository.saveAuthorizationRequest(first, request(null), response);
        repository.saveAuthorizationRequest(second, request(null), response);

        assertThat(repository.removeAuthorizationRequest(request("first-state"), response).getState())
                .isEqualTo("first-state");
        assertThat(repository.loadAuthorizationRequest(request("first-state"))).isNull();
        assertThat(repository.loadAuthorizationRequest(request("second-state")).getState())
                .isEqualTo("second-state");
    }

    private MockHttpServletRequest request(String state) {
        MockHttpServletRequest request = new MockHttpServletRequest();
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
