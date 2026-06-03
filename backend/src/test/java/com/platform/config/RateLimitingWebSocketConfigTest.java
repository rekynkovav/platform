package com.platform.config;

import com.platform.websocket.BroadcastStatsWebSocketHandler;
import com.platform.websocket.ChatWebSocketHandler;
import com.platform.websocket.WebSocketHandshakeInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.socket.config.annotation.ServletWebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitingWebSocketConfigTest {

    // ==================== RateLimitingConfig ====================

    @Test
    void rateLimitingConfig_Bucket4jRedisTemplate_ReturnsNonNull() {
        RateLimitingConfig config = new RateLimitingConfig();
        LettuceConnectionFactory connectionFactory = mock(LettuceConnectionFactory.class);

        RedisTemplate<String, Object> template = config.bucket4jRedisTemplate(connectionFactory);

        assertNotNull(template);
    }

    @Test
    void rateLimitingConfig_Bucket4jRedisTemplate_UsesProvidedConnectionFactory() {
        RateLimitingConfig config = new RateLimitingConfig();
        LettuceConnectionFactory connectionFactory = mock(LettuceConnectionFactory.class);

        RedisTemplate<String, Object> template = config.bucket4jRedisTemplate(connectionFactory);

        assertSame(connectionFactory, template.getConnectionFactory());
    }

    // ==================== WebSocketConfig ====================

    @Test
    void webSocketConfig_RegisterHandlers_DoesNotThrow() {
        ChatWebSocketHandler chatHandler = mock(ChatWebSocketHandler.class);
        BroadcastStatsWebSocketHandler broadcastHandler = mock(BroadcastStatsWebSocketHandler.class);
        WebSocketHandshakeInterceptor interceptor = mock(WebSocketHandshakeInterceptor.class);

        WebSocketConfig config = new WebSocketConfig(chatHandler, broadcastHandler, interceptor);
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);

        ServletWebSocketHandlerRegistration registration = mock(ServletWebSocketHandlerRegistration.class);
        when(registration.addInterceptors(any())).thenReturn(registration);
        when(registration.setAllowedOrigins(any(String[].class))).thenReturn(registration);
        when(registration.withSockJS()).thenReturn(null);

        when(registry.addHandler(any(), eq("/ws/chat"))).thenReturn(registration);
        when(registry.addHandler(any(), eq("/ws/broadcast/{broadcastId}/stats"))).thenReturn(registration);

        assertDoesNotThrow(() -> config.registerWebSocketHandlers(registry));
    }

    @Test
    void webSocketConfig_ImplementsWebSocketConfigurer() {
        ChatWebSocketHandler chatHandler = mock(ChatWebSocketHandler.class);
        BroadcastStatsWebSocketHandler broadcastHandler = mock(BroadcastStatsWebSocketHandler.class);
        WebSocketHandshakeInterceptor interceptor = mock(WebSocketHandshakeInterceptor.class);

        WebSocketConfig config = new WebSocketConfig(chatHandler, broadcastHandler, interceptor);

        assertInstanceOf(org.springframework.web.socket.config.annotation.WebSocketConfigurer.class, config);
    }
}
