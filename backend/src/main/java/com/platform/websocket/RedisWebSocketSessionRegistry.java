package com.platform.websocket;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedisWebSocketSessionRegistry {

    private static final String SESSION_PREFIX = "ws:session:";
    private static final String USER_SESSIONS_PREFIX = "ws:user:";
    private static final String ALL_SESSIONS_KEY = "ws:sessions:all";
    private static final long SESSION_TTL = 24 * 60 * 60; // 24 часа

    private final RedisTemplate<String, String> redisTemplate;
    private final Map<String, WebSocketSession> localSessions = new ConcurrentHashMap<>();

    public RedisWebSocketSessionRegistry(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void registerSession(String userId, String sessionId, WebSocketSession session) {
        localSessions.put(sessionId, session);
        redisTemplate.opsForHash().put(USER_SESSIONS_PREFIX + userId, sessionId, "active");
        redisTemplate.expire(USER_SESSIONS_PREFIX + userId, SESSION_TTL, TimeUnit.SECONDS);
        redisTemplate.opsForSet().add(SESSION_PREFIX + sessionId, userId);
        redisTemplate.expire(SESSION_PREFIX + sessionId, SESSION_TTL, TimeUnit.SECONDS);
        redisTemplate.opsForSet().add(ALL_SESSIONS_KEY, sessionId);

        log.debug("Session registered: userId={}, sessionId={}", userId, sessionId);
    }

    public void unregisterSession(String userId, String sessionId) {
        localSessions.remove(sessionId);
        redisTemplate.opsForHash().delete(USER_SESSIONS_PREFIX + userId, sessionId);
        redisTemplate.delete(SESSION_PREFIX + sessionId);
        redisTemplate.opsForSet().remove(ALL_SESSIONS_KEY, sessionId);

        log.debug("Session unregistered: userId={}, sessionId={}", userId, sessionId);
    }

    public void sendToUser(String userId, String message) {
        Map<Object, Object> userSessions = redisTemplate.opsForHash().entries(USER_SESSIONS_PREFIX + userId);

        for (Object sessionIdObj : userSessions.keySet()) {
            String sessionId = sessionIdObj.toString();
            WebSocketSession session = localSessions.get(sessionId);

            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
                }
            }
        }
    }

    public void sendToAll(String message) {
        Set<String> sessionIds = redisTemplate.opsForSet().members(ALL_SESSIONS_KEY);
        if (sessionIds == null) return;

        for (String sessionId : sessionIds) {
            WebSocketSession session = localSessions.get(sessionId);

            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
                }
            }
        }
    }

    public void sendToRoom(String roomId, String message) {
        // Отправка всем в комнату через Redis pub/sub
        redisTemplate.convertAndSend("ws:room:" + roomId, message);
    }

    public boolean isUserOnline(String userId) {
        Long size = redisTemplate.opsForHash().size(USER_SESSIONS_PREFIX + userId);
        return size != null && size > 0;
    }

    public int getOnlineCount() {
        Long size = redisTemplate.opsForSet().size(ALL_SESSIONS_KEY);
        return size != null ? size.intValue() : 0;
    }
}