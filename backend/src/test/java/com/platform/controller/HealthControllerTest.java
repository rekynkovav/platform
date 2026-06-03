package com.platform.controller;

import com.platform.websocket.ChatWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    private MockMvc mockMvc;

    @Mock private ChatWebSocketHandler chatWebSocketHandler;

    @InjectMocks
    private HealthController healthController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(healthController).build();
    }

    // ==================== GET /api/health ====================

    @Test
    void health_ReturnsOkWithStructure() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.ffmpeg").exists())
                .andExpect(jsonPath("$.timestamp").isNumber());
    }

    @Test
    void health_AlwaysReturnsTimestamp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").isNumber());
    }

    // ==================== GET /api/health/websocket ====================

    @Test
    void websocketHealth_Healthy_ReturnsUp() throws Exception {
        when(chatWebSocketHandler.isHealthy()).thenReturn(true);
        when(chatWebSocketHandler.getActiveSessionCount()).thenReturn(5);

        mockMvc.perform(get("/api/health/websocket"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.activeSessions").value(5))
                .andExpect(jsonPath("$.timestamp").isNumber());
    }

    @Test
    void websocketHealth_Unhealthy_ReturnsDegraded() throws Exception {
        when(chatWebSocketHandler.isHealthy()).thenReturn(false);
        when(chatWebSocketHandler.getActiveSessionCount()).thenReturn(0);

        mockMvc.perform(get("/api/health/websocket"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.activeSessions").value(0));
    }

    @Test
    void websocketHealth_NoAuthRequired_Returns200() throws Exception {
        when(chatWebSocketHandler.isHealthy()).thenReturn(true);
        when(chatWebSocketHandler.getActiveSessionCount()).thenReturn(0);

        mockMvc.perform(get("/api/health/websocket"))
                .andExpect(status().isOk());
    }
}
