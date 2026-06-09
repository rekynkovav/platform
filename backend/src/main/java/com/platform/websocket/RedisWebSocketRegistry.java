package com.platform.websocket;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedisWebSocketRegistry {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisWebSocketRegistry(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void register(String userId, String sessionId) {
        redisTemplate.opsForHash().put("ws:user:" + userId, sessionId, "active");
        redisTemplate.expire("ws:user:" + userId, 24, TimeUnit.HOURS);
        redisTemplate.convertAndSend("ws:register", userId + ":" + sessionId);
        log.debug("Registered WebSocket session: {} for user: {}", sessionId, userId);
    }

    public void unregister(String userId, String sessionId) {
        redisTemplate.opsForHash().delete("ws:user:" + userId, sessionId);
        redisTemplate.convertAndSend("ws:unregister", userId + ":" + sessionId);
        log.debug("Unregistered WebSocket session: {} for user: {}", sessionId, userId);
    }

    public void sendToUser(String userId, String message) {
        redisTemplate.convertAndSend("ws:message:" + userId, message);
        log.debug("Sent message to user: {} via Redis", userId);
    }

    public boolean isUserOnline(String userId) {
        Long count = redisTemplate.opsForHash().size("ws:user:" + userId);
        return count != null && count > 0;
    }

    public void broadcastToRoom(String roomId, String message) {
        redisTemplate.convertAndSend("ws:room:" + roomId, message);
        log.debug("Broadcast message to room: {} via Redis", roomId);
    }

    public void cleanupUserSessions(String userId) {
        redisTemplate.delete("ws:user:" + userId);
        log.debug("Cleaned up all sessions for user: {}", userId);
    }
}