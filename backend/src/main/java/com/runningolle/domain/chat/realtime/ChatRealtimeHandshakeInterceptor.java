package com.runningolle.domain.chat.realtime;

import com.runningolle.global.security.jwt.JwtTokenProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
@RequiredArgsConstructor
public class ChatRealtimeHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String token = jwtTokenProvider.resolveTokenFromCookieHeader(
                request.getHeaders().getFirst(HttpHeaders.COOKIE)
        );

        if (token == null || token.isBlank() || !jwtTokenProvider.validateToken(token)) {
            setUnauthorized(response);
            return false;
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(token);
        attributes.put("userId", authentication.getName());
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }

    private void setUnauthorized(ServerHttpResponse response) {
        if (response instanceof ServletServerHttpResponse servletResponse) {
            servletResponse.getServletResponse().setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }
}
