package com.platform.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisWebSocketSessionRegistryTest {

    private RedisTemplate<String, String> redisTemplate;
    private HashOperations<String, Object, Object> hashOps;
    private SetOperations<String, String> setOps;
    private RedisWebSocketSessionRegistry registry;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        hashOps = mock(HashOperations.class);
        setOps = mock(SetOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        registry = new RedisWebSocketSessionRegistry(redisTemplate);
    }

    // ==================== registerSession ====================

    @Test
    void registerSession_StoresInRedisAndLocal() {
        WebSocketSession session = mock(WebSocketSession.class);
        registry.registerSession("user-1", "sess-1", session);

        verify(hashOps).put(eq("ws:user:user-1"), eq("sess-1"), eq("active"));
        verify(redisTemplate).expire(eq("ws:user:user-1"), anyLong(), any());
        verify(setOps).add(eq("ws:session:sess-1"), eq("user-1"));
        verify(redisTemplate).expire(eq("ws:session:sess-1"), anyLong(), any());
    }

    // ==================== unregisterSession ====================

    @Test
    void unregisterSession_RemovesFromRedisAndLocal() {
        WebSocketSession session = mock(WebSocketSession.class);
        registry.registerSession("user-1", "sess-1", session);
        registry.unregisterSession("user-1", "sess-1");

        verify(hashOps).delete("ws:user:user-1", "sess-1");
        verify(redisTemplate).delete("ws:session:sess-1");
    }

    // ==================== sendToUser ====================

    @Test
    void sendToUser_OpenSession_SendsMessage() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        registry.registerSession("user-1", "sess-1", session);

        Map<Object, Object> sessions = new HashMap<>();
        sessions.put("sess-1", "active");
        when(hashOps.entries("ws:user:user-1")).thenReturn(sessions);

        registry.sendToUser("user-1", "hello");

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void sendToUser_ClosedSession_SkipsMessage() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(false);
        registry.registerSession("user-1", "sess-2", session);

        Map<Object, Object> sessions = new HashMap<>();
        sessions.put("sess-2", "active");
        when(hashOps.entries("ws:user:user-1")).thenReturn(sessions);

        registry.sendToUser("user-1", "hello");

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void sendToUser_NoSessions_DoesNotThrow() {
        when(hashOps.entries("ws:user:nobody")).thenReturn(new HashMap<>());

        assertDoesNotThrow(() -> registry.sendToUser("nobody", "msg"));
    }

    // ==================== sendToRoom ====================

    @Test
    void sendToRoom_PublishesToRedisChannel() {
        registry.sendToRoom("room-1", "room message");

        verify(redisTemplate).convertAndSend("ws:room:room-1", "room message");
    }

    // ==================== isUserOnline ====================

    @Test
    void isUserOnline_TrueWhenSessionsExist() {
        when(hashOps.size("ws:user:user-1")).thenReturn(1L);
        assertTrue(registry.isUserOnline("user-1"));
    }

    @Test
    void isUserOnline_FalseWhenNoSessions() {
        when(hashOps.size("ws:user:user-2")).thenReturn(0L);
        assertFalse(registry.isUserOnline("user-2"));
    }

    @Test
    void isUserOnline_NullSize_ReturnsFalse() {
        when(hashOps.size("ws:user:user-3")).thenReturn(null);
        assertFalse(registry.isUserOnline("user-3"));
    }

    // ==================== getOnlineCount ====================

    @Test
    void getOnlineCount_ReturnsKeyCount() {
        when(setOps.size("ws:sessions:all")).thenReturn(3L);
        assertEquals(3, registry.getOnlineCount());
    }

    @Test
    void getOnlineCount_NullKeys_ReturnsZero() {
        when(setOps.size("ws:sessions:all")).thenReturn(null);
        assertEquals(0, registry.getOnlineCount());
    }
}
