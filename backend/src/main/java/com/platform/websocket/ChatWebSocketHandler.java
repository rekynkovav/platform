package com.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.model.dto.MessageDto;
import com.platform.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    // Хранилище активных сессий: userId -> session
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // Хранилище сессий по ID сессии
    private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("userId");

        if (userId == null) {
            log.warn("WebSocket connection without userId, closing");
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (IOException e) {
                log.error("Error closing session", e);
            }
            return;
        }

        sessions.put(userId, session);
        sessionToUser.put(session.getId(), userId);

        log.info("WebSocket connected: userId={}, sessionId={}, total sessions={}",
                userId, session.getId(), sessions.size());

        // Отправляем приветственное сообщение
        sendMessageToUser(userId, MessageDto.builder()
                .type("connection")
                .content("Connected to chat server")
                .build());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String userId = sessionToUser.get(session.getId());
        if (userId == null) return;

        try {
            ChatMessagePayload payload = objectMapper.readValue(
                    message.getPayload(),
                    ChatMessagePayload.class
            );

            MessageDto savedMessage;

            switch (payload.getType()) {
                case "general":
                    savedMessage = chatService.sendToGeneralChat(
                            userId,
                            payload.getContent()
                    );
                    broadcastToAll(savedMessage);
                    break;

                case "admin_group":
                    savedMessage = chatService.sendToAdminGroup(
                            userId,
                            payload.getContent()
                    );
                    broadcastToAdmins(savedMessage);
                    break;

                case "private":
                    savedMessage = chatService.sendPrivateMessage(
                            userId,
                            payload.getRecipientId(),
                            payload.getContent()
                    );
                    sendMessageToUser(userId, savedMessage);
                    sendMessageToUser(payload.getRecipientId(), savedMessage);
                    break;

                default:
                    log.warn("Unknown message type: {}", payload.getType());
            }

        } catch (Exception e) {
            log.error("Error processing WebSocket message", e);
            sendErrorMessage(session, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = sessionToUser.remove(session.getId());
        if (userId != null) {
            sessions.remove(userId);
            log.info("WebSocket disconnected: userId={}, sessionId={}, total sessions={}",
                    userId, session.getId(), sessions.size());
        }
    }

    private void sendMessageToUser(String userId, MessageDto message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                log.error("Error sending message to user: {}", userId, e);
            }
        }
    }

    private void broadcastToAll(MessageDto message) {
        sessions.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                } catch (IOException e) {
                    log.error("Error broadcasting to user: {}", userId, e);
                }
            }
        });
    }

    private void broadcastToAdmins(MessageDto message) {
        // TODO: Получить список админов из сервиса
        // Пока отправляем всем
        broadcastToAll(message);
    }

    private void sendErrorMessage(WebSocketSession session, String error) {
        try {
            session.sendMessage(new TextMessage(
                    objectMapper.writeValueAsString(Map.of(
                            "type", "error",
                            "content", error
                    ))
            ));
        } catch (IOException e) {
            log.error("Error sending error message", e);
        }
    }

    // Внутренний класс для парсинга сообщений
    private static class ChatMessagePayload {
        private String type;
        private String content;
        private String recipientId;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getRecipientId() { return recipientId; }
        public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    }
}
