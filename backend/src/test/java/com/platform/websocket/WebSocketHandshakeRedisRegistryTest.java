package com.platform.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WebSocketHandshakeRedisRegistryTest {

    // ==================== WebSocketHandshakeInterceptor ====================

    private JwtDecoder jwtDecoder;
    private WebSocketHandshakeInterceptor interceptor;
    private ServerHttpRequest request;
    private ServerHttpResponse response;
    private WebSocketHandler wsHandler;

    @BeforeEach
    void setUp() {
        jwtDecoder = mock(JwtDecoder.class);
        interceptor = new WebSocketHandshakeInterceptor(jwtDecoder);
        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);
        wsHandler = mock(WebSocketHandler.class);
    }

    @Test
    void beforeHandshake_ValidBearerToken_Authenticates() {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", List.of("Bearer valid-token"));
        when(request.getHeaders()).thenReturn(headers);

        Jwt jwt = Jwt.withTokenValue("valid-token")
                .header("alg", "none")
                .subject("user-1")
                .claim("realm_access.roles", "USER")
                .build();
        when(jwtDecoder.decode("valid-token")).thenReturn(jwt);

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertTrue(result);
        assertEquals("user-1", attributes.get("userId"));
        assertEquals("valid-token", attributes.get("token"));
    }

    @Test
    void beforeHandshake_TokenInQueryParam_Authenticates() {
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(request.getURI()).thenReturn(URI.create("ws://localhost/chat?token=query-token"));

        Jwt jwt = Jwt.withTokenValue("query-token")
                .header("alg", "none")
                .subject("user-2")
                .claim("realm_access.roles", "USER")
                .build();
        when(jwtDecoder.decode("query-token")).thenReturn(jwt);

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertTrue(result);
        assertEquals("user-2", attributes.get("userId"));
    }

    @Test
    void beforeHandshake_NoToken_ReturnsFalse() {
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(request.getURI()).thenReturn(URI.create("ws://localhost/chat"));

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertFalse(result);
    }

    @Test
    void beforeHandshake_InvalidToken_ReturnsFalse() {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", List.of("Bearer bad-token"));
        when(request.getHeaders()).thenReturn(headers);
        when(jwtDecoder.decode("bad-token")).thenThrow(new JwtException("invalid"));

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertFalse(result);
    }

    @Test
    void afterHandshake_WithException_DoesNotThrow() {
        assertDoesNotThrow(() ->
                interceptor.afterHandshake(request, response, wsHandler, new RuntimeException("test")));
    }

    @Test
    void afterHandshake_NullException_DoesNotThrow() {
        assertDoesNotThrow(() ->
                interceptor.afterHandshake(request, response, wsHandler, null));
    }

    // ==================== RedisWebSocketRegistry ====================

    @SuppressWarnings("unchecked")
    @Test
    void redisRegistry_Register_PutsHashAndPublishes() {
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashOps);

        RedisWebSocketRegistry registry = new RedisWebSocketRegistry(redis);
        registry.register("user-1", "session-1");

        verify(hashOps).put(eq("ws:user:user-1"), eq("session-1"), eq("active"));
        verify(redis).convertAndSend("ws:register", "user-1:session-1");
    }

    @SuppressWarnings("unchecked")
    @Test
    void redisRegistry_Unregister_DeletesHashAndPublishes() {
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashOps);

        RedisWebSocketRegistry registry = new RedisWebSocketRegistry(redis);
        registry.unregister("user-1", "session-1");

        verify(hashOps).delete(eq("ws:user:user-1"), eq("session-1"));
        verify(redis).convertAndSend("ws:unregister", "user-1:session-1");
    }

    @SuppressWarnings("unchecked")
    @Test
    void redisRegistry_IsUserOnline_TrueWhenSessionsExist() {
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashOps);
        when(hashOps.size("ws:user:user-1")).thenReturn(2L);

        RedisWebSocketRegistry registry = new RedisWebSocketRegistry(redis);
        assertTrue(registry.isUserOnline("user-1"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void redisRegistry_IsUserOnline_FalseWhenNoSessions() {
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashOps);
        when(hashOps.size("ws:user:user-2")).thenReturn(0L);

        RedisWebSocketRegistry registry = new RedisWebSocketRegistry(redis);
        assertFalse(registry.isUserOnline("user-2"));
    }

    @Test
    void redisRegistry_CleanupUserSessions_DeletesKey() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);

        RedisWebSocketRegistry registry = new RedisWebSocketRegistry(redis);
        registry.cleanupUserSessions("user-3");

        verify(redis).delete("ws:user:user-3");
    }
}
