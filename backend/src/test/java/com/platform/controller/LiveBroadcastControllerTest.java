package com.platform.controller;

import com.platform.config.TestSecurityConfig;
import com.platform.model.dto.LiveBroadcastDto;
import com.platform.service.LiveBroadcastService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LiveBroadcastController.class)
@Import(TestSecurityConfig.class)
class LiveBroadcastControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LiveBroadcastService liveBroadcastService;

    @Test
    void testGetActiveBroadcasts_ReturnsActiveBroadcasts() throws Exception {
        LiveBroadcastDto broadcast = LiveBroadcastDto.builder()
                .id("broadcast-123")
                .title("Test Broadcast")
                .description("Test Description")
                .isLive(true)
                .viewerCount(100)
                .build();

        when(liveBroadcastService.getActiveBroadcasts()).thenReturn(List.of(broadcast));

        mockMvc.perform(get("/api/broadcast/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("broadcast-123"))
                .andExpect(jsonPath("$[0].title").value("Test Broadcast"))
                .andExpect(jsonPath("$[0].isLive").value(true));

        verify(liveBroadcastService).getActiveBroadcasts();
    }

    @Test
    @WithMockUser(username = "streamer-123", roles = {"USER_2"})
    void testCreateBroadcast_WithAuth_CreatesBroadcast() throws Exception {
        LiveBroadcastDto created = LiveBroadcastDto.builder()
                .id("new-broadcast")
                .title("New Stream")
                .description("Stream description")
                .isLive(false)
                .build();

        when(liveBroadcastService.createBroadcast(any(), eq("streamer-123"))).thenReturn(created);

        mockMvc.perform(post("/api/broadcast/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "title": "New Stream",
                                "description": "Stream description"
                            }
                            """)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("new-broadcast"))
                .andExpect(jsonPath("$.title").value("New Stream"));

        verify(liveBroadcastService).createBroadcast(any(), eq("streamer-123"));
    }

    @Test
    void testCreateBroadcast_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(post("/api/broadcast/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Test\"}")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(liveBroadcastService, never()).createBroadcast(any(), any());
    }

    @Test
    void testHandleRtmpWebhook_WithoutSecret_Returns403() throws Exception {
        mockMvc.perform(post("/api/broadcast/webhook/rtmp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "action": "publish",
                                "app": "live",
                                "stream": "test-stream"
                            }
                            """))
                .andExpect(status().isForbidden());

        verify(liveBroadcastService, never()).onStreamStart(any());
    }

    @Test
    void testHandleRtmpWebhook_WithWrongSecret_Returns403() throws Exception {
        mockMvc.perform(post("/api/broadcast/webhook/rtmp")
                        .header("X-Webhook-Secret", "wrong-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "action": "publish",
                                "app": "live",
                                "stream": "test-stream"
                            }
                            """))
                .andExpect(status().isForbidden());

        verify(liveBroadcastService, never()).onStreamStart(any());
    }

    @Test
    void testHandleRtmpWebhook_WithCorrectSecret_CallsOnStreamStart() throws Exception {
        // В реальном приложении секрет должен быть в application-test.yml
        // Для теста используем заглушку
        doNothing().when(liveBroadcastService).onStreamStart(any());

        mockMvc.perform(post("/api/broadcast/webhook/rtmp")
                        .header("X-Webhook-Secret", "test-webhook-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "action": "publish",
                                "app": "live",
                                "stream": "test-stream-123",
                                "clientId": "client-123"
                            }
                            """))
                .andExpect(status().isOk());

        verify(liveBroadcastService).onStreamStart(any());
    }

    @Test
    @WithMockUser(username = "streamer-123", roles = {"USER_2"})
    void testStopBroadcast_StopsBroadcast() throws Exception {
        LiveBroadcastDto stopped = LiveBroadcastDto.builder()
                .id("broadcast-123")
                .title("Stopped Stream")
                .isLive(false)
                .build();

        when(liveBroadcastService.stopBroadcast(eq("broadcast-123"), eq("streamer-123")))
                .thenReturn(stopped);

        mockMvc.perform(post("/api/broadcast/broadcast-123/stop")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("broadcast-123"))
                .andExpect(jsonPath("$.isLive").value(false));

        verify(liveBroadcastService).stopBroadcast(eq("broadcast-123"), eq("streamer-123"));
    }
}
