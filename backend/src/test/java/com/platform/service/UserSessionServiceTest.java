package com.platform.service;

import com.platform.websocket.ChatWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserSessionServiceTest {

    private UserSessionService userSessionService;
    private RedisTemplate<String, String> redisTemplate;
    private ChatWebSocketHandler webSocketHandler;
    private ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        webSocketHandler = mock(ChatWebSocketHandler.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        userSessionService = new UserSessionService(redisTemplate, webSocketHandler);
    }

    // ==================== invalidateAllSessions ====================

    @Test
    void invalidateAllSessions_ClosesWebSocketAndBlacklistsToken() {
        String userId = "user-123";
        String expectedKey = "blacklisted_tokens:" + userId;

        userSessionService.invalidateAllSessions(userId);

        verify(webSocketHandler).closeUserSessions(userId);
        verify(valueOps).set(eq(expectedKey), anyString());
        verify(redisTemplate).expire(eq(expectedKey), eq(Duration.ofHours(24)));
    }

    @Test
    void invalidateAllSessions_SetsTimestampAsValue() {
        String userId = "user-456";
        long before = System.currentTimeMillis();

        userSessionService.invalidateAllSessions(userId);

        verify(valueOps).set(anyString(), argThat(val -> {
            long ts = Long.parseLong(val);
            return ts >= before && ts <= System.currentTimeMillis();
        }));
    }

    @Test
    void invalidateAllSessions_DifferentUsers_DifferentKeys() {
        userSessionService.invalidateAllSessions("user-A");
        userSessionService.invalidateAllSessions("user-B");

        verify(valueOps).set(eq("blacklisted_tokens:user-A"), anyString());
        verify(valueOps).set(eq("blacklisted_tokens:user-B"), anyString());
    }

    // ==================== isTokenBlacklisted ====================

    @Test
    void isTokenBlacklisted_KeyExists_ReturnsTrue() {
        when(redisTemplate.hasKey("blacklisted_tokens:user-1")).thenReturn(true);

        assertTrue(userSessionService.isTokenBlacklisted("user-1"));
    }

    @Test
    void isTokenBlacklisted_KeyNotExists_ReturnsFalse() {
        when(redisTemplate.hasKey("blacklisted_tokens:user-2")).thenReturn(false);

        assertFalse(userSessionService.isTokenBlacklisted("user-2"));
    }

    @Test
    void isTokenBlacklisted_NullResult_ReturnsFalse() {
        when(redisTemplate.hasKey("blacklisted_tokens:user-3")).thenReturn(null);

        assertFalse(userSessionService.isTokenBlacklisted("user-3"));
    }
}
