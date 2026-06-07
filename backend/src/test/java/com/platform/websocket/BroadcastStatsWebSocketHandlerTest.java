package com.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.service.LiveBroadcastService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BroadcastStatsWebSocketHandlerTest {

    private LiveBroadcastService broadcastService;
    private ObjectMapper objectMapper;
    private BroadcastStatsWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        broadcastService = mock(LiveBroadcastService.class);
        objectMapper = new ObjectMapper();
        handler = new BroadcastStatsWebSocketHandler(broadcastService, objectMapper);
    }

    // ==================== afterConnectionEstablished ====================

    @Test
    void afterConnectionEstablished_ValidPath_AddsViewerAndSendsStats() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/broadcast/bc-1/stats"));
        when(session.getId()).thenReturn("sess-1");
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);

        verify(broadcastService).addViewer("bc-1");
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void afterConnectionEstablished_InvalidPath_DoesNothing() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/invalid"));

        handler.afterConnectionEstablished(session);

        verifyNoInteractions(broadcastService);
    }

    // ==================== afterConnectionClosed ====================

    @Test
    void afterConnectionClosed_RemovesViewer() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/broadcast/bc-2/stats"));
        when(session.getId()).thenReturn("sess-2");
        when(session.isOpen()).thenReturn(true);

        // First establish connection
        handler.afterConnectionEstablished(session);
        // Then close it
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(broadcastService).removeViewer("bc-2");
    }

    @Test
    void afterConnectionClosed_InvalidPath_DoesNothing() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/short"));

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verifyNoInteractions(broadcastService);
    }

    // ==================== sendStatsToAll ====================

    @Test
    void sendStatsToAll_NoSessions_DoesNotThrow() {
        assertDoesNotThrow(() -> handler.sendStatsToAll("nonexistent-bc"));
    }

    @Test
    void sendStatsToAll_WithSessions_SendsToAllOpen() throws Exception {
        WebSocketSession s1 = mock(WebSocketSession.class);
        WebSocketSession s2 = mock(WebSocketSession.class);
        when(s1.getUri()).thenReturn(URI.create("ws://localhost/ws/broadcast/bc-3/stats"));
        when(s1.getId()).thenReturn("s1");
        when(s1.isOpen()).thenReturn(true);
        when(s2.getUri()).thenReturn(URI.create("ws://localhost/ws/broadcast/bc-3/stats"));
        when(s2.getId()).thenReturn("s2");
        when(s2.isOpen()).thenReturn(false); // closed

        handler.afterConnectionEstablished(s1);
        handler.afterConnectionEstablished(s2);

        // Reset to count only sendStatsToAll calls
        reset(s1, s2);
        when(s1.isOpen()).thenReturn(true);
        when(s2.isOpen()).thenReturn(false);

        handler.sendStatsToAll("bc-3");

        verify(s1).sendMessage(any(TextMessage.class));
        verify(s2, never()).sendMessage(any(TextMessage.class));
    }

    // ==================== cleanup ====================

    @Test
    void cleanup_ClosesAllSessionsAndClearsMap() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/broadcast/bc-4/stats"));
        when(session.getId()).thenReturn("sess-4");
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);
        handler.cleanup();

        verify(session).close();
    }

    @Test
    void cleanup_EmptyMap_DoesNotThrow() {
        assertDoesNotThrow(handler::cleanup);
    }
}
