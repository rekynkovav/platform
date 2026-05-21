package com.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.service.LiveBroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class BroadcastStatsWebSocketHandler extends TextWebSocketHandler {

    private final LiveBroadcastService broadcastService;
    private final ObjectMapper objectMapper;

    // broadcastId -> список сессий
    private final Map<String, Map<String, WebSocketSession>> broadcastSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String broadcastId = getBroadcastId(session);
        if (broadcastId == null) {
            return;
        }

        broadcastSessions.computeIfAbsent(broadcastId, k -> new ConcurrentHashMap<>())
                .put(session.getId(), session);

        broadcastService.addViewer(broadcastId);
        sendStatsToAll(broadcastId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String broadcastId = getBroadcastId(session);
        if (broadcastId == null) {
            return;
        }

        Map<String, WebSocketSession> sessions = broadcastSessions.get(broadcastId);
        if (sessions != null) {
            sessions.remove(session.getId());
            if (sessions.isEmpty()) {
                broadcastSessions.remove(broadcastId);
            }
        }

        broadcastService.removeViewer(broadcastId);
        sendStatsToAll(broadcastId);
    }

    public void sendStatsToAll(String broadcastId) {
        Map<String, WebSocketSession> sessions = broadcastSessions.get(broadcastId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            int viewerCount = sessions.size();
            String message = objectMapper.writeValueAsString(Map.of(
                "type", "stats",
                "broadcastId", broadcastId,
                "viewerCount", viewerCount,
                "timestamp", System.currentTimeMillis()
            ));

            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private String getBroadcastId(WebSocketSession session) {
        String path = session.getUri().getPath();
        // /ws/broadcast/{id}/stats
        String[] parts = path.split("/");
        if (parts.length >= 4) {
            return parts[3];
        }
        return null;
    }

    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up WebSocket sessions");
        log.info("Очистка WebSocket сессий");

        for (Map<String, WebSocketSession> sessions : broadcastSessions.values()) {
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
        broadcastSessions.clear();
    }

    private static final Logger log = LoggerFactory.getLogger(BroadcastStatsWebSocketHandler.class);
}