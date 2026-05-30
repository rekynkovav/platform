package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.model.dto.LiveBroadcastDto;
import com.platform.model.enums.BroadcastStatus;
import com.platform.service.LiveBroadcastService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LiveBroadcastControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private LiveBroadcastService liveBroadcastService;

    @InjectMocks
    private LiveBroadcastController liveBroadcastController;

    private String userId;
    private UUID userIdUuid;
    private String broadcastId;
    private UUID broadcastIdUuid;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(liveBroadcastController).build();
        objectMapper = new ObjectMapper();

        userId = "user-123";
        userIdUuid = UUID.fromString(userId);
        broadcastId = "broadcast-456";
        broadcastIdUuid = UUID.fromString(broadcastId);
    }

    @Test
    void createBroadcast_ValidRequest_ReturnsBroadcastDto() throws Exception {
        // Arrange
        String title = "My Live Stream";
        String description = "Testing live broadcast";
        LiveBroadcastDto expectedResponse = LiveBroadcastDto.builder()
                .id(broadcastIdUuid)
                .title(title)
                .description(description)
                .ownerId(userIdUuid)
                .ownerName("TestUser")
                .status(BroadcastStatus.IDLE)
                .rtmpStreamKey(broadcastId)
                .createdAt(Instant.now())
                .build();

        when(liveBroadcastService.createBroadcast(eq(title), eq(description), eq(userId)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/broadcasts")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"" + title + "\", \"description\": \"" + description + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(broadcastId))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.description").value(description))
                .andExpect(jsonPath("$.status").value("IDLE"))
                .andExpect(jsonPath("$.rtmpStreamKey").value(broadcastId));
    }

    @Test
    void createBroadcast_WithoutDescription_ReturnsBroadcastDto() throws Exception {
        // Arrange
        String title = "My Live Stream";
        LiveBroadcastDto expectedResponse = LiveBroadcastDto.builder()
                .id(broadcastIdUuid)
                .title(title)
                .ownerId(userIdUuid)
                .ownerName("TestUser")
                .status(BroadcastStatus.IDLE)
                .rtmpStreamKey(broadcastId)
                .createdAt(Instant.now())
                .build();

        when(liveBroadcastService.createBroadcast(eq(title), isNull(), eq(userId)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/broadcasts")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"" + title + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    void createBroadcast_EmptyTitle_ReturnsBadRequest() throws Exception {
        // Arrange
        String emptyTitle = "";

        // Act & Assert
        mockMvc.perform(post("/api/broadcasts")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"" + emptyTitle + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startBroadcast_ValidRequest_ReturnsRtmpUrl() throws Exception {
        // Arrange
        String rtmpUrl = "rtmp://live.example.com/app/" + broadcastId;
        when(liveBroadcastService.startBroadcast(eq(broadcastId), eq(userId)))
                .thenReturn(rtmpUrl);

        // Act & Assert
        mockMvc.perform(post("/api/broadcasts/{id}/start", broadcastId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().string(rtmpUrl));
    }

    @Test
    void startBroadcast_NotOwner_ReturnsForbidden() throws Exception {
        // Arrange
        String otherUserId = "other-789";
        when(liveBroadcastService.startBroadcast(eq(broadcastId), eq(otherUserId)))
                .thenThrow(new com.platform.exception.AccessDeniedException("Access denied"));

        // Act & Assert
        mockMvc.perform(post("/api/broadcasts/{id}/start", broadcastId)
                        .header("X-User-Id", otherUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    void stopBroadcast_ValidRequest_ReturnsNoContent() throws Exception {
        // Arrange
        when(liveBroadcastService.stopBroadcast(eq(broadcastId), eq(userId))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/broadcasts/{id}/stop", broadcastId)
                        .header("X-User-Id", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    void getBroadcast_ValidRequest_ReturnsBroadcastDto() throws Exception {
        // Arrange
        LiveBroadcastDto expectedResponse = LiveBroadcastDto.builder()
                .id(broadcastIdUuid)
                .title("Live Stream")
                .description("Test broadcast")
                .ownerId(userIdUuid)
                .ownerName("TestUser")
                .status(BroadcastStatus.LIVE)
                .viewerCount(150)
                .maxViewers(200)
                .startedAt(Instant.now().minusMinutes(30))
                .createdAt(Instant.now().minusHours(1))
                .build();

        when(liveBroadcastService.getBroadcast(eq(broadcastId))).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/broadcasts/{id}", broadcastId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIVE"))
                .andExpect(jsonPath("$.viewerCount").value(150))
                .andExpect(jsonPath("$.maxViewers").value(200));
    }

    @Test
    void getBroadcast_NotFound_ReturnsNotFound() throws Exception {
        // Arrange
        String nonExistentBroadcastId = "non-existent-broadcast";
        when(liveBroadcastService.getBroadcast(eq(nonExistentBroadcastId)))
                .thenThrow(new com.platform.exception.ResourceNotFoundException("Broadcast not found"));

        // Act & Assert
        mockMvc.perform(get("/api/broadcasts/{id}", nonExistentBroadcastId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getHlsPlaylist_ValidRequest_ReturnsHlsUrl() throws Exception {
        // Arrange
        String hlsUrl = "http://cdn.example.com/hls/" + broadcastId + "/index.m3u8";
        when(liveBroadcastService.getHlsPlaylist(eq(broadcastId))).thenReturn(hlsUrl);

        // Act & Assert
        mockMvc.perform(get("/api/broadcasts/{id}/hls", broadcastId))
                .andExpect(status().isOk())
                .andExpect(content().string(hlsUrl));
    }

    @Test
    void getHlsPlaylist_NotLive_ReturnsBadRequest() throws Exception {
        // Arrange
        when(liveBroadcastService.getHlsPlaylist(eq(broadcastId)))
                .thenThrow(new IllegalStateException("Broadcast is not live"));

        // Act & Assert
        mockMvc.perform(get("/api/broadcasts/{id}/hls", broadcastId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getActiveBroadcasts_ValidRequest_ReturnsBroadcasts() throws Exception {
        // Arrange
        LiveBroadcastDto broadcast1 = LiveBroadcastDto.builder()
                .id(UUID.randomUUID())
                .title("Stream 1")
                .ownerId(userIdUuid)
                .ownerName("User1")
                .status(BroadcastStatus.LIVE)
                .viewerCount(100)
                .startedAt(Instant.now().minusMinutes(45))
                .build();

        LiveBroadcastDto broadcast2 = LiveBroadcastDto.builder()
                .id(UUID.randomUUID())
                .title("Stream 2")
                .ownerId(UUID.randomUUID())
                .ownerName("User2")
                .status(BroadcastStatus.LIVE)
                .viewerCount(50)
                .startedAt(Instant.now().minusMinutes(20))
                .build();

        List<LiveBroadcastDto> expectedResponse = List.of(broadcast1, broadcast2);

        when(liveBroadcastService.getActiveBroadcasts()).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/broadcasts/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("LIVE"))
                .andExpect(jsonPath("$[1].status").value("LIVE"));
    }

    @Test
    void getActiveBroadcasts_Empty_ReturnsEmptyList() throws Exception {
        // Arrange
        when(liveBroadcastService.getActiveBroadcasts()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/broadcasts/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void addViewer_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        when(liveBroadcastService.addViewer(eq(broadcastId))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/broadcasts/{id}/viewer", broadcastId))
                .andExpect(status().isOk());
    }

    @Test
    void addViewer_BroadcastNotFound_ReturnsNotFound() throws Exception {
        // Arrange
        String nonExistentBroadcastId = "non-existent-broadcast";
        when(liveBroadcastService.addViewer(eq(nonExistentBroadcastId)))
                .thenThrow(new com.platform.exception.ResourceNotFoundException("Broadcast not found"));

        // Act & Assert
        mockMvc.perform(post("/api/broadcasts/{id}/viewer", nonExistentBroadcastId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBroadcast_ValidRequest_ReturnsNoContent() throws Exception {
        // Arrange
        when(liveBroadcastService.deleteBroadcast(eq(broadcastId), eq(userId))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/broadcasts/{id}", broadcastId)
                        .header("X-User-Id", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteBroadcast_NotOwner_ReturnsForbidden() throws Exception {
        // Arrange
        String otherUserId = "other-789";
        when(liveBroadcastService.deleteBroadcast(eq(broadcastId), eq(otherUserId)))
                .thenThrow(new com.platform.exception.AccessDeniedException("Access denied"));

        // Act & Assert
        mockMvc.perform(delete("/api/broadcasts/{id}", broadcastId)
                        .header("X-User-Id", otherUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserBroadcasts_ValidRequest_ReturnsBroadcasts() throws Exception {
        // Arrange
        LiveBroadcastDto broadcast1 = LiveBroadcastDto.builder()
                .id(UUID.randomUUID())
                .title("My Stream 1")
                .ownerId(userIdUuid)
                .status(BroadcastStatus.ENDED)
                .startedAt(Instant.now().minusDays(1))
                .endedAt(Instant.now().minusDays(1).plusHours(2))
                .build();

        LiveBroadcastDto broadcast2 = LiveBroadcastDto.builder()
                .id(UUID.randomUUID())
                .title("My Stream 2")
                .ownerId(userIdUuid)
                .status(BroadcastStatus.IDLE)
                .createdAt(Instant.now().minusHours(3))
                .build();

        List<LiveBroadcastDto> expectedResponse = List.of(broadcast1, broadcast2);

        when(liveBroadcastService.getUserBroadcasts(eq(userId))).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/broadcasts/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ownerId").value(userId))
                .andExpect(jsonPath("$[1].ownerId").value(userId));
    }

    @Test
    void getUserBroadcasts_NoBroadcasts_ReturnsEmptyList() throws Exception {
        // Arrange
        when(liveBroadcastService.getUserBroadcasts(eq(userId))).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/broadcasts/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}