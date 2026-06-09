package com.platform.config;

import com.platform.websocket.BroadcastStatsWebSocketHandler;
import com.platform.websocket.ChatWebSocketHandler;
import com.platform.websocket.WebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final BroadcastStatsWebSocketHandler broadcastStatsWebSocketHandler;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Чат с поддержкой заголовков и CORS
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins(
                    "http://localhost:3000",
                    "http://localhost:3001",
                    "https://platform.local"
                )
                .withSockJS();

        // Статистика трансляций
        registry.addHandler(broadcastStatsWebSocketHandler, "/ws/broadcast/{broadcastId}/stats")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins(
                    "http://localhost:3000",
                    "http://localhost:3001",
                    "https://platform.local"
                );
    }
}