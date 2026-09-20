package com.runningolle.global.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.StringUtils;

/**
 * Keeps concurrent OAuth login attempts separate by their state parameter.
 *
 * <p>Spring Security's default session repository stores only one authorization
 * request. A second login attempt can therefore overwrite the first one before
 * Kakao redirects it back.</p>
 */
public class StateAwareOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String SESSION_ATTRIBUTE =
            StateAwareOAuth2AuthorizationRequestRepository.class.getName() + ".AUTHORIZATION_REQUESTS";
    private static final int MAX_PENDING_REQUESTS = 5;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = request.getParameter("state");
        if (!StringUtils.hasText(state)) {
            return null;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        synchronized (session) {
            return getAuthorizationRequests(session).get(state);
        }
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }

        String state = authorizationRequest.getState();
        if (!StringUtils.hasText(state)) {
            throw new IllegalArgumentException("authorizationRequest.state cannot be empty");
        }

        HttpSession session = request.getSession();
        synchronized (session) {
            Map<String, OAuth2AuthorizationRequest> requests = getAuthorizationRequests(session);
            requests.put(state, authorizationRequest);
            while (requests.size() > MAX_PENDING_REQUESTS) {
                String oldestState = requests.keySet().iterator().next();
                requests.remove(oldestState);
            }
            session.setAttribute(SESSION_ATTRIBUTE, requests);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String state = request.getParameter("state");
        if (!StringUtils.hasText(state)) {
            return null;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        synchronized (session) {
            Map<String, OAuth2AuthorizationRequest> requests = getAuthorizationRequests(session);
            OAuth2AuthorizationRequest removed = requests.remove(state);
            if (requests.isEmpty()) {
                session.removeAttribute(SESSION_ATTRIBUTE);
            } else {
                session.setAttribute(SESSION_ATTRIBUTE, requests);
            }
            return removed;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, OAuth2AuthorizationRequest> getAuthorizationRequests(HttpSession session) {
        Object value = session.getAttribute(SESSION_ATTRIBUTE);
        if (value instanceof Map<?, ?>) {
            return (Map<String, OAuth2AuthorizationRequest>) value;
        }
        return new LinkedHashMap<>();
    }
}
