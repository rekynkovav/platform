package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.model.LiveBroadcast;
import com.platform.service.LiveBroadcastService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(LiveBroadcastController.class)
class LiveBroadcastControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveBroadcastService broadcastService;

    private LiveBroadcast testBroadcast;
    private String userId;

    @BeforeEach
    void setUp() {
        userId = "user123";
        testBroadcast = LiveBroadcast.builder()
            .id("broadcast123")
            .title("Test Broadcast")
            .description("Test Description")
            .streamType("video")
            .ownerId(userId)
            .status("idle")
            .build();
    }

    @Test
    @WithMockUser(roles = {"CREATOR"})
    void testCreateBroadcast() throws Exception {
        when(broadcastService.createBroadcast(any(), any(), any(), any()))
            .thenReturn(testBroadcast);

        Map<String, String> request = Map.of(
            "title", "Test Broadcast",
            "description", "Test Description",
            "streamType", "video"
        );

        mockMvc.perform(post("/api/broadcast/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("broadcast123"))
                .andExpect(jsonPath("$.title").value("Test Broadcast"));

        verify(broadcastService, times(1)).createBroadcast(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"CREATOR"})
    void testStartBroadcast() throws Exception {
        when(broadcastService.startBroadcast(eq("broadcast123"), any()))
            .thenReturn("rtmp://localhost/live/streamKey");

        mockMvc.perform(post("/api/broadcast/broadcast123/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("live"));

        verify(broadcastService, times(1)).startBroadcast(eq("broadcast123"), any());
    }

    @Test
    @WithMockUser(roles = {"CREATOR"})
    void testStopBroadcast() throws Exception {
        doNothing().when(broadcastService).stopBroadcast(eq("broadcast123"), any());

        mockMvc.perform(post("/api/broadcast/broadcast123/stop"))
                .andExpect(status().isOk());

        verify(broadcastService, times(1)).stopBroadcast(eq("broadcast123"), any());
    }

    @Test
    void testGetActiveBroadcasts() throws Exception {
        when(broadcastService.getActiveBroadcasts()).thenReturn(List.of(testBroadcast));

        mockMvc.perform(get("/api/broadcast/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("broadcast123"));

        verify(broadcastService, times(1)).getActiveBroadcasts();
    }

    @Test
    void testGetBroadcast() throws Exception {
        when(broadcastService.getBroadcast("broadcast123")).thenReturn(testBroadcast);

        mockMvc.perform(get("/api/broadcast/broadcast123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Broadcast"));

        verify(broadcastService, times(1)).getBroadcast("broadcast123");
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testDeleteBroadcast() throws Exception {
        doNothing().when(broadcastService).deleteBroadcast(eq("broadcast123"), any());

        mockMvc.perform(delete("/api/broadcast/broadcast123"))
                .andExpect(status().isOk());

        verify(broadcastService, times(1)).deleteBroadcast(eq("broadcast123"), any());
    }
}