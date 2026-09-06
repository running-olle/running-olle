package com.runningolle.global.config;

import com.runningolle.domain.chat.realtime.ChatRealtimeHandshakeInterceptor;
import com.runningolle.domain.chat.realtime.ChatListWebSocketHandler;
import com.runningolle.domain.chat.realtime.ChatRoomWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatRoomWebSocketHandler chatRoomWebSocketHandler;
    private final ChatListWebSocketHandler chatListWebSocketHandler;
    private final ChatRealtimeHandshakeInterceptor chatRealtimeHandshakeInterceptor;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatRoomWebSocketHandler, "/ws/community/chats/{roomId}")
                .addInterceptors(chatRealtimeHandshakeInterceptor)
                .setAllowedOrigins(allowedOrigins());
        registry.addHandler(chatListWebSocketHandler, "/ws/community/chat-list")
                .addInterceptors(chatRealtimeHandshakeInterceptor)
                .setAllowedOrigins(allowedOrigins());
    }

    private String[] allowedOrigins() {
        return new String[] {
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:8080",
                "http://127.0.0.1:8080",
                normalizeOrigin(frontendUrl)
        };
    }

    private String normalizeOrigin(String origin) {
        return origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
    }
}
