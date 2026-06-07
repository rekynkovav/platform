package com.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.model.Notification;
import com.platform.model.dto.MessageDto;
import com.platform.service.ChatService;
import com.platform.service.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatWebSocketHandlerTest {

    private ChatService chatService;
    private ObjectMapper objectMapper;
    private MetricsService metricsService;
    private ChatWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        objectMapper = new ObjectMapper();
        metricsService = mock(MetricsService.class);
        handler = new ChatWebSocketHandler(chatService, objectMapper, metricsService);
    }

    // ==================== afterConnectionEstablished ====================

    @Test
    void afterConnectionEstablished_WithUserId_RegistersSessionAndIncrementsMetrics() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", "user-1");
        when(session.getAttributes()).thenReturn(attrs);
        when(session.getId()).thenReturn("sess-1");

        handler.afterConnectionEstablished(session);

        verify(metricsService).incrementWebSocketConnections();
        assertEquals(1, handler.getActiveSessionCount());
    }

    @Test
    void afterConnectionEstablished_WithoutUserId_StillIncrementsMetrics() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(new HashMap<>());
        when(session.getId()).thenReturn("sess-2");

        handler.afterConnectionEstablished(session);

        verify(metricsService).incrementWebSocketConnections();
    }

    // ==================== afterConnectionClosed ====================

    @Test
    void afterConnectionClosed_RemovesSessionAndDecrementsMetrics() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", "user-2");
        when(session.getAttributes()).thenReturn(attrs);
        when(session.getId()).thenReturn("sess-3");

        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(metricsService).decrementWebSocketConnections();
        assertEquals(0, handler.getActiveSessionCount());
    }

    // ==================== handleTransportError ====================

    @Test
    void handleTransportError_IncrementsErrorsAndClosesSession() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);

        handler.handleTransportError(session, new RuntimeException("test error"));

        verify(metricsService).incrementWebSocketErrors();
        verify(session).close(CloseStatus.SERVER_ERROR);
    }

    // ==================== sendNotificationToUser ====================

    @Test
    void sendNotificationToUser_OpenSession_SendsMessage() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", "user-3");
        when(session.getAttributes()).thenReturn(attrs);
        when(session.getId()).thenReturn("sess-4");
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setType("INFO");
        notification.setMessage("test notification");

        handler.sendNotificationToUser("user-3", notification);

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void sendNotificationToUser_NoSession_DoesNotThrow() {
        Notification notification = new Notification();
        notification.setType("INFO");
        notification.setMessage("test");

        assertDoesNotThrow(() -> handler.sendNotificationToUser("nonexistent", notification));
    }

    // ==================== isHealthy / getActiveSessionCount ====================

    @Test
    void isHealthy_ReturnsTrue() {
        assertTrue(handler.isHealthy());
    }

    @Test
    void getActiveSessionCount_InitiallyZero() {
        assertEquals(0, handler.getActiveSessionCount());
    }

    // ==================== resetMessageCounts ====================

    @Test
    void resetMessageCounts_DoesNotThrow() {
        assertDoesNotThrow(() -> handler.resetMessageCounts());
    }

    // ==================== cleanStaleSessions ====================

    @Test
    void cleanStaleSessions_RemovesClosedSessions() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", "user-stale");
        when(session.getAttributes()).thenReturn(attrs);
        when(session.getId()).thenReturn("sess-stale");
        when(session.isOpen()).thenReturn(false);

        handler.afterConnectionEstablished(session);
        assertEquals(1, handler.getActiveSessionCount());

        handler.cleanStaleSessions();
        assertEquals(0, handler.getActiveSessionCount());
    }

    // ==================== ChatMessagePayload ====================

    @Test
    void chatMessagePayload_GettersSettersWork() {
        ChatWebSocketHandler.ChatMessagePayload payload = new ChatWebSocketHandler.ChatMessagePayload();
        payload.setRoomSlug("room-1");
        payload.setUserId("user-1");
        payload.setContent("hello");
        payload.setMediaId("media-1");
        payload.setUserRole("USER");

        assertEquals("room-1", payload.getRoomSlug());
        assertEquals("user-1", payload.getUserId());
        assertEquals("hello", payload.getContent());
        assertEquals("media-1", payload.getMediaId());
        assertEquals("USER", payload.getUserRole());
    }
}
