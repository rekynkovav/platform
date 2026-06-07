package com.platform.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WebSocketComponentsTest {

    // ==================== RedisWebSocketRegistry ====================

    private RedisWebSocketRegistry registry;
    private RedisTemplate<String, String> redisTemplate;
    private HashOperations<String, Object, Object> hashOps;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        registry = new RedisWebSocketRegistry(redisTemplate);
    }

    @Test
    void register_PutsSessionInRedisAndPublishes() {
        registry.register("user-1", "session-abc");

        verify(hashOps).put("ws:user:user-1", "session-abc", "active");
        verify(redisTemplate).convertAndSend("ws:register", "user-1:session-abc");
    }

    @Test
    void unregister_RemovesSessionAndPublishes() {
        registry.unregister("user-1", "session-abc");

        verify(hashOps).delete("ws:user:user-1", "session-abc");
        verify(redisTemplate).convertAndSend("ws:unregister", "user-1:session-abc");
    }

    @Test
    void sendToUser_PublishesMessage() {
        registry.sendToUser("user-1", "{\"text\":\"hello\"}");

        verify(redisTemplate).convertAndSend("ws:message:user-1", "{\"text\":\"hello\"}");
    }

    @Test
    void isUserOnline_HasSessions_ReturnsTrue() {
        when(hashOps.size("ws:user:user-1")).thenReturn(2L);

        assertTrue(registry.isUserOnline("user-1"));
    }

    @Test
    void isUserOnline_NoSessions_ReturnsFalse() {
        when(hashOps.size("ws:user:user-2")).thenReturn(0L);

        assertFalse(registry.isUserOnline("user-2"));
    }

    @Test
    void isUserOnline_NullResult_ReturnsFalse() {
        when(hashOps.size("ws:user:user-3")).thenReturn(null);

        assertFalse(registry.isUserOnline("user-3"));
    }

    @Test
    void broadcastToRoom_PublishesToRoomChannel() {
        registry.broadcastToRoom("room-42", "{\"event\":\"new_msg\"}");

        verify(redisTemplate).convertAndSend("ws:room:room-42", "{\"event\":\"new_msg\"}");
    }

    @Test
    void cleanupUserSessions_DeletesAllUserKeys() {
        registry.cleanupUserSessions("user-1");

        verify(redisTemplate).delete("ws:user:user-1");
    }

    // ==================== WebSocketHandshakeInterceptor (unit-level) ====================

    @Test
    void afterHandshake_WithException_DoesNotThrow() {
        var interceptor = new WebSocketHandshakeInterceptor(token -> {
            throw new UnsupportedOperationException();
        });

        assertDoesNotThrow(() ->
                interceptor.afterHandshake(null, null, null, new RuntimeException("test")));
    }

    @Test
    void afterHandshake_WithoutException_DoesNotThrow() {
        var interceptor = new WebSocketHandshakeInterceptor(token -> {
            throw new UnsupportedOperationException();
        });

        assertDoesNotThrow(() ->
                interceptor.afterHandshake(null, null, null, null));
    }
}
