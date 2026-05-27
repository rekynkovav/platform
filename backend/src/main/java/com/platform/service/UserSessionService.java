package com.platform.service;

import com.platform.websocket.ChatWebSocketHandler;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class UserSessionService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ChatWebSocketHandler webSocketHandler;

    public UserSessionService(RedisTemplate<String, String> redisTemplate, ChatWebSocketHandler webSocketHandler) {
        this.redisTemplate = redisTemplate;
        this.webSocketHandler = webSocketHandler;
    }

    public void invalidateAllSessions(String userId) {
        // 1. Закрыть WebSocket соединения
        webSocketHandler.closeUserSessions(userId);

        // 2. Добавить токен в черный список
        String blacklistKey = "blacklisted_tokens:" + userId;
        redisTemplate.opsForValue().set(blacklistKey, String.valueOf(System.currentTimeMillis()));
        redisTemplate.expire(blacklistKey, Duration.ofHours(24));
    }

    public boolean isTokenBlacklisted(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklisted_tokens:" + userId));
    }
}