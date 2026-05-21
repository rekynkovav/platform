package com.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.model.Notification;
import com.platform.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    // Чаты: сессии по roomSlug
    private final Map<String, Map<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    // Уведомления: сессии по userId
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.put(userId, session);
        }
        System.out.println("WebSocket connected → user: " + userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        ChatMessagePayload payloadObj = objectMapper.readValue(payload, ChatMessagePayload.class);

        String roomSlug = payloadObj.getRoomSlug();

        // Добавляем сессию в комнату
        roomSessions.computeIfAbsent(roomSlug, k -> new ConcurrentHashMap<>())
                .put(session.getId(), session);

        // Сохраняем сообщение
        MessageDto saved = chatService.sendMessageToRoom(
                roomSlug,
                payloadObj.getUserId(),
                payloadObj.getContent(),
                payloadObj.getMediaId(),
                (String) session.getAttributes().get("userRole") // роль из JWT
        );

        // Рассылаем сообщение всем в комнате
        broadcastToRoom(roomSlug, saved);
    }

    public void sendNotificationToUser(String userId, Notification notification) {
        WebSocketSession session = userSessions.get(userId);
        if (session == null || !session.isOpen()) return;

        try {
            String json = objectMapper.writeValueAsString(notification);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            System.err.println("Failed to send notification to " + userId);
        }
    }

    private void broadcastToRoom(String roomSlug, MessageDto message) {
        Map<String, WebSocketSession> sessions = roomSessions.get(roomSlug);
        if (sessions == null) return;

        sessions.values().removeIf(s -> !s.isOpen());

        try {
            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);

            for (WebSocketSession s : sessions.values()) {
                s.sendMessage(textMessage);
            }
        } catch (Exception e) {
            // silent
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.remove(userId);
        }
        roomSessions.values().forEach(map -> map.remove(session.getId()));
    }

    // Payload для сообщений чата
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

    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up WebSocket sessions");
        log.info("Очистка WebSocket сессий");

        for (Map<String, WebSocketSession> sessions : roomSessions.values()) {
            for (WebSocketSession session : sessions.values()) {
                try {
                    if (session.isOpen()) {
                        session.close();
                    }
                } catch (Exception e) {
                    log.warn("Error closing session: {}", e.getMessage());
                }
            }
        }
        roomSessions.clear();

        for (WebSocketSession session : userSessions.values()) {
            try {
                if (session.isOpen()) {
                    session.close();
                }
            } catch (Exception e) {
                log.warn("Error closing session: {}", e.getMessage());
            }
        }
        userSessions.clear();
    }

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
}