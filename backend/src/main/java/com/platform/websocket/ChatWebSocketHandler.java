package com.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.platform.model.Notification;
import com.platform.model.dto.MessageDto;
import com.platform.service.ChatService;
import com.platform.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    private static final int MAX_SESSIONS_PER_ROOM = 10000;
    private static final int MAX_SESSIONS_TOTAL = 100000;
    private static final int MAX_MESSAGES_PER_MINUTE = 60;

    private final ExecutorService broadcastExecutor = new ThreadPoolExecutor(
        4, 16, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(500),
        new ThreadPoolExecutor.CallerRunsPolicy());

    private final Map<String, Map<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> messageCounts = new ConcurrentHashMap<>();
    private final Queue<PendingMessage> pendingMessages = new ArrayBlockingQueue<>(1000);
    private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(2);

    private final Cache<String, Map<String, WebSocketSession>> roomSessionsCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            Set<WebSocketSession> sessions = userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>());
            sessions.add(session);
        }
        log.info("WebSocket connected: {}, total sessions: {}", session.getId(), userSessions.size());
        if (metricsService != null) {
            metricsService.incrementWebSocketConnections();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: {}", exception.getMessage());
        if (metricsService != null) {
            metricsService.incrementWebSocketErrors();
        }
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException e) {
            log.error("Failed to close session after error", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        ChatMessagePayload payloadObj = objectMapper.readValue(payload, ChatMessagePayload.class);

        String userId = getUserId(session);
        AtomicInteger count = messageCounts.computeIfAbsent(userId, k -> new AtomicInteger(0));

        if (count.incrementAndGet() > MAX_MESSAGES_PER_MINUTE) {
            log.warn("Rate limit exceeded for user: {}", userId);
            session.sendMessage(new TextMessage("{\"error\":\"Rate limit exceeded. Max 60 messages per minute\"}"));
            return;
        }

        String roomSlug = payloadObj.getRoomSlug();

        Map<String, WebSocketSession> room = roomSessions.computeIfAbsent(roomSlug, k -> new ConcurrentHashMap<>());
        if (room.size() >= MAX_SESSIONS_PER_ROOM) {
            session.sendMessage(new TextMessage("{\"error\":\"Room is full\"}"));
            return;
        }
        room.put(session.getId(), session);

        MessageDto saved = chatService.sendMessageToRoom(
                roomSlug,
                userId,
                payloadObj.getContent(),
                payloadObj.getMediaId(),
                (String) session.getAttributes().get("userRole")
        );

        broadcastToRoomAsync(roomSlug, saved);
    }

    private void broadcastToRoomAsync(String roomSlug, MessageDto message) {
        Map<String, WebSocketSession> sessions = roomSessions.get(roomSlug);
        if (sessions == null || sessions.isEmpty()) return;

        sessions.values().removeIf(s -> !s.isOpen());

        try {
            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);

            for (WebSocketSession s : sessions.values()) {
                broadcastExecutor.submit(() -> {
                    if (s.isOpen()) {
                        sendWithRetry(s, textMessage, 0);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Broadcast failed", e);
        }
    }

    public void sendNotificationToUser(String userId, Notification notification) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) return;

        try {
            String json = objectMapper.writeValueAsString(notification);
            TextMessage textMessage = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.warn("Failed to send notification to session {}", session.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to send notification to {}", userId);
        }
    }

    public void sendNotificationToUser(String userId, String message) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) return;

        try {
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.warn("Failed to send notification to session {}", session.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to send notification to {}", userId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        }
        roomSessions.values().forEach(map -> map.remove(session.getId()));
        log.info("WebSocket closed: {}, status: {}, active sessions: {}", session.getId(), status, userSessions.size());
        if (metricsService != null) {
            metricsService.decrementWebSocketConnections();
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("WebSocketHandler shutting down, closing {} user sessions", userSessions.size());
        broadcastExecutor.shutdown();
        retryExecutor.shutdown();
        try {
            if (!broadcastExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                broadcastExecutor.shutdownNow();
            }
            if (!retryExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                retryExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            broadcastExecutor.shutdownNow();
            retryExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        for (Set<WebSocketSession> sessions : userSessions.values()) {
            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {
                        session.close(CloseStatus.GOING_AWAY);
                    }
                } catch (IOException e) {
                    log.error("Error closing WebSocket session: {}", e.getMessage());
                }
            }
        }
        userSessions.clear();
        roomSessions.clear();
        log.info("All WebSocket sessions closed");
    }

    @Scheduled(fixedRate = 60000)
    public void resetMessageCounts() {
        messageCounts.clear();
        log.debug("Rate limit counters reset");
    }

    @Scheduled(fixedRate = 300000)
    public void cleanStaleSessions() {
        int before = userSessions.size();
        userSessions.values().forEach(set -> set.removeIf(s -> !s.isOpen()));
        userSessions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        roomSessions.values().forEach(map -> map.values().removeIf(s -> !s.isOpen()));
        log.info("Cleaned stale sessions: {} -> {}", before, userSessions.size());
    }

    @Scheduled(fixedRate = 30000)
    public void sendHeartbeats() {
        for (Set<WebSocketSession> sessions : userSessions.values()) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new PingMessage());
                    } catch (IOException e) {
                        log.debug("Failed to send heartbeat to session {}", session.getId());
                    }
                }
            }
        }
    }

    private String getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId != null ? userId.toString() : "anonymous";
    }

    public static class ChatMessagePayload {
        private String roomSlug;
        private String userId;
        private String content;
        private String mediaId;
        private String userRole;

        public String getRoomSlug() { return roomSlug; }
        public void setRoomSlug(String roomSlug) { this.roomSlug = roomSlug; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getMediaId() { return mediaId; }
        public void setMediaId(String mediaId) { this.mediaId = mediaId; }
        public String getUserRole() { return userRole; }
        public void setUserRole(String userRole) { this.userRole = userRole; }
    }

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private static class PendingMessage {
        final WebSocketSession session;
        final TextMessage message;
        final int attempt;
        final long timestamp;

        PendingMessage(WebSocketSession session, TextMessage message, int attempt) {
            this.session = session;
            this.message = message;
            this.attempt = attempt;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private void sendWithRetry(WebSocketSession session, TextMessage message, int attempt) {
        try {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        } catch (IOException e) {
            if (attempt < 3) {
                boolean offered = pendingMessages.offer(new PendingMessage(session, message, attempt + 1));
                if (!offered) {
                    log.warn("Pending message queue full, dropping retry for session: {}", session.getId());
                    return;
                }
                retryExecutor.schedule(() -> processPendingMessages(),
                    (long) Math.pow(2, attempt) * 1000, TimeUnit.MILLISECONDS);
                log.warn("Message delivery failed, scheduled retry {} for session: {}", attempt + 1, session.getId());
            } else {
                log.error("Message delivery failed after {} attempts for session: {}", attempt, session.getId());
            }
        }
    }

    private void processPendingMessages() {
        PendingMessage pending;
        while ((pending = pendingMessages.poll()) != null) {
            if (System.currentTimeMillis() - pending.timestamp > 30000) {
                log.warn("Pending message expired after 30 seconds");
                continue;
            }
            sendWithRetry(pending.session, pending.message, pending.attempt);
        }
    }

    public void closeUserSessions(String userId) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null) return;
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.close(CloseStatus.GOING_AWAY);
                }
            } catch (IOException e) {
                log.warn("Failed to close session for user {}: {}", userId, e.getMessage());
            }
        }
        userSessions.remove(userId);
    }

    public boolean isHealthy() {
        // Проверка, что WebSocket обработчик работает
        return true;
    }

    public int getActiveSessionCount() {
        return userSessions.size();
    }
}