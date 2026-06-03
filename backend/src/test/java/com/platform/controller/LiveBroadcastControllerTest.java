package com.platform.controller;

import com.platform.dto.CreateBroadcastRequest;
import com.platform.model.LiveBroadcast;
import com.platform.model.dto.LiveBroadcastDto;
import com.platform.service.BroadcastArchiveService;
import com.platform.service.LiveBroadcastService;
import com.platform.service.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LiveBroadcastControllerTest {

    private LiveBroadcastController controller;
    private LiveBroadcastService broadcastService;
    private MetricsService metricsService;
    private BroadcastArchiveService archiveService;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        broadcastService = mock(LiveBroadcastService.class);
        metricsService = mock(MetricsService.class);
        archiveService = mock(BroadcastArchiveService.class);
        controller = new LiveBroadcastController(broadcastService, metricsService, archiveService);
        jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-123")
                .build();
    }

    // ==================== createAndStartBroadcast ====================

    @Test
    void createAndStart_ReturnsStreamUrlAndKey() {
        LiveBroadcast broadcast = new LiveBroadcast();
        broadcast.setId(UUID.randomUUID());
        when(broadcastService.createBroadcast(anyString(), anyString(), anyString(), eq("user-123")))
                .thenReturn(broadcast);
        when(broadcastService.startBroadcast(eq(broadcast.getId().toString()), eq("user-123")))
                .thenReturn("rtmp://server/live/key");

        CreateBroadcastRequest request = new CreateBroadcastRequest();
        request.setTitle("Test Stream");
        request.setDescription("Desc");
        request.setStreamType("rtmp");

        ResponseEntity<Map<String, String>> response = controller.createAndStartBroadcast(request, jwt);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("rtmp://server/live/key", response.getBody().get("streamUrl"));
        assertEquals("live", response.getBody().get("status"));
        verify(metricsService).incrementBroadcasts();
    }

    // ==================== startBroadcast ====================

    @Test
    void startBroadcast_ReturnsStreamInfo() {
        when(broadcastService.startBroadcast("bc-1", "user-123"))
                .thenReturn("rtmp://server/live/bc-1");

        ResponseEntity<Map<String, String>> response = controller.startBroadcast("bc-1", jwt);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("rtmp://server/live/bc-1", response.getBody().get("streamUrl"));
        assertEquals("bc-1", response.getBody().get("streamKey"));
        verify(metricsService).incrementBroadcasts();
    }

    // ==================== stopBroadcast / endBroadcast ====================

    @Test
    void stopBroadcast_ReturnsOk() {
        ResponseEntity<Void> response = controller.stopBroadcast("bc-1", jwt);

        assertEquals(200, response.getStatusCode().value());
        verify(broadcastService).stopBroadcast("bc-1", "user-123");
    }

    @Test
    void endBroadcast_DelegatesToStop() {
        ResponseEntity<Void> response = controller.endBroadcast("bc-1", jwt);

        assertEquals(200, response.getStatusCode().value());
        verify(broadcastService).stopBroadcast("bc-1", "user-123");
    }

    // ==================== getActiveBroadcasts ====================

    @Test
    void getActiveBroadcasts_ReturnsMappedDtos() {
        LiveBroadcast bc = new LiveBroadcast();
        bc.setId(UUID.randomUUID());
        LiveBroadcastDto dto = new LiveBroadcastDto();
        when(broadcastService.getActiveBroadcasts()).thenReturn(List.of(bc));
        when(broadcastService.toDto(bc)).thenReturn(dto);

        ResponseEntity<List<LiveBroadcastDto>> response = controller.getActiveBroadcasts();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    // ==================== getBroadcastStats ====================

    @Test
    void getBroadcastStats_ReturnsStatsMap() {
        LiveBroadcast bc = new LiveBroadcast();
        bc.setId(UUID.randomUUID());
        LiveBroadcastDto dto = new LiveBroadcastDto();
        dto.setStatus("live");
        dto.setViewerCount(42);
        dto.setStartedAt(LocalDateTime.now());
        when(broadcastService.getBroadcast("bc-1")).thenReturn(bc);
        when(broadcastService.toDto(bc)).thenReturn(dto);

        ResponseEntity<Map<String, Object>> response = controller.getBroadcastStats("bc-1", jwt);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("live", response.getBody().get("status"));
        assertEquals(42, response.getBody().get("viewerCount"));
    }

    @Test
    void getBroadcastStats_NullFields_ReturnsDefaults() {
        LiveBroadcast bc = new LiveBroadcast();
        LiveBroadcastDto dto = new LiveBroadcastDto();
        when(broadcastService.getBroadcast("bc-2")).thenReturn(bc);
        when(broadcastService.toDto(bc)).thenReturn(dto);

        ResponseEntity<Map<String, Object>> response = controller.getBroadcastStats("bc-2", jwt);

        assertEquals("UNKNOWN", response.getBody().get("status"));
        assertEquals(0, response.getBody().get("viewerCount"));
        assertEquals("", response.getBody().get("startedAt"));
    }

    // ==================== deleteBroadcast ====================

    @Test
    void deleteBroadcast_ReturnsOk() {
        ResponseEntity<Void> response = controller.deleteBroadcast("bc-1", jwt);

        assertEquals(200, response.getStatusCode().value());
        verify(broadcastService).deleteBroadcast("bc-1", "user-123");
    }

    // ==================== rtmpWebhook ====================

    @Test
    void rtmpWebhook_PublishAction_CallsOnStreamStart() {
        Map<String, String> payload = Map.of("action", "publish", "streamKey", "sk-1");

        ResponseEntity<Void> response = controller.rtmpWebhook(payload, null);

        assertEquals(200, response.getStatusCode().value());
        verify(broadcastService).onStreamStart("sk-1");
    }

    @Test
    void rtmpWebhook_PublishDoneAction_CallsOnStreamStop() {
        Map<String, String> payload = Map.of("action", "publish_done", "streamKey", "sk-2");

        ResponseEntity<Void> response = controller.rtmpWebhook(payload, null);

        assertEquals(200, response.getStatusCode().value());
        verify(broadcastService).onStreamStop("sk-2");
    }
}
