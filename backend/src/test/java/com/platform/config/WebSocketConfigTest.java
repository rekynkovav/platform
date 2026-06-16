package com.platform.config;

import com.platform.websocket.BroadcastStatsWebSocketHandler;
import com.platform.websocket.ChatWebSocketHandler;
import com.platform.websocket.WebSocketHandshakeInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketConfigTest {

    @Test
    void webSocketConfig_RegisterHandlers_RegistersChatAndBroadcast() {
        ChatWebSocketHandler chatHandler = mock(ChatWebSocketHandler.class);
        BroadcastStatsWebSocketHandler broadcastHandler = mock(BroadcastStatsWebSocketHandler.class);
        WebSocketHandshakeInterceptor interceptor = mock(WebSocketHandshakeInterceptor.class);

        WebSocketConfig config = new WebSocketConfig(chatHandler, broadcastHandler, interceptor);
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);

        var chatRegistration = mock(org.springframework.web.socket.config.annotation.ServletWebSocketHandlerRegistration.class);
        var broadcastRegistration = mock(org.springframework.web.socket.config.annotation.ServletWebSocketHandlerRegistration.class);

        when(registry.addHandler(eq(chatHandler), eq("/ws/chat"))).thenReturn(chatRegistration);
        when(chatRegistration.addInterceptors(any())).thenReturn(chatRegistration);
        when(chatRegistration.setAllowedOrigins(anyString(), anyString(), anyString())).thenReturn(chatRegistration);

        when(registry.addHandler(eq(broadcastHandler), eq("/ws/broadcast/{broadcastId}/stats"))).thenReturn(broadcastRegistration);
        when(broadcastRegistration.addInterceptors(any())).thenReturn(broadcastRegistration);
        when(broadcastRegistration.setAllowedOrigins(anyString(), anyString(), anyString())).thenReturn(broadcastRegistration);

        assertDoesNotThrow(() -> config.registerWebSocketHandlers(registry));

        verify(registry).addHandler(chatHandler, "/ws/chat");
        verify(registry).addHandler(broadcastHandler, "/ws/broadcast/{broadcastId}/stats");
    }
}
