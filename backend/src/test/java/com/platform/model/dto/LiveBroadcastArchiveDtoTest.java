package com.platform.model.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LiveBroadcastArchiveDtoTest {

    // ==================== LiveBroadcastDto ====================

    @Test
    void liveBroadcastDto_SettersAndGetters_Work() {
        LiveBroadcastDto dto = new LiveBroadcastDto();
        LocalDateTime now = LocalDateTime.now();

        dto.setId("lb-1");
        dto.setTitle("Live Stream");
        dto.setDescription("Streaming now");
        dto.setStreamType("RTMP");
        dto.setUserId("user-1");
        dto.setStatus("LIVE");
        dto.setCreatedAt(now);
        dto.setStartedAt(now);
        dto.setEndedAt(null);
        dto.setViewerCount(150);
        dto.setHlsPlaylistUrl("https://cdn.example.com/live.m3u8");
        dto.setThumbnailUrl("https://cdn.example.com/thumb.jpg");

        assertEquals("lb-1", dto.getId());
        assertEquals("Live Stream", dto.getTitle());
        assertEquals("RTMP", dto.getStreamType());
        assertEquals("LIVE", dto.getStatus());
        assertEquals(150, dto.getViewerCount());
        assertEquals("https://cdn.example.com/live.m3u8", dto.getHlsPlaylistUrl());
        assertNull(dto.getEndedAt());
    }

    @Test
    void liveBroadcastDto_DoesNotExposeSensitiveFields() {
        // Verify that sensitive fields like streamKey, encryptionKey are NOT present
        LiveBroadcastDto dto = new LiveBroadcastDto();
        // The class should only have safe client-facing fields
        assertNotNull(dto.getClass().getDeclaredFields());
        // No streamKey field should exist
        assertThrows(NoSuchFieldException.class, () ->
                dto.getClass().getDeclaredField("streamKey"));
        assertThrows(NoSuchFieldException.class, () ->
                dto.getClass().getDeclaredField("encryptionKey"));
    }

    // ==================== ArchiveDto ====================

    @Test
    void archiveDto_Builder_SetsAllFields() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 20, 18, 0);
        LocalDateTime end = LocalDateTime.of(2026, 5, 20, 20, 0);

        ArchiveDto dto = ArchiveDto.builder()
                .id("arch-1")
                .title("Past Event Recording")
                .description("Full recording")
                .videoUrl("https://cdn.example.com/recording.mp4")
                .thumbnailUrl("https://cdn.example.com/arch-thumb.jpg")
                .duration(7200)
                .viewsCount(500)
                .startTime(start)
                .endTime(end)
                .eventId("ev-1")
                .createdAt(LocalDateTime.now())
                .build();

        assertEquals("arch-1", dto.getId());
        assertEquals("Past Event Recording", dto.getTitle());
        assertEquals(7200, dto.getDuration());
        assertEquals(500, dto.getViewsCount());
        assertEquals(start, dto.getStartTime());
        assertEquals(end, dto.getEndTime());
        assertEquals("ev-1", dto.getEventId());
    }

    @Test
    void archiveDto_CreateRequest_BuilderWorks() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 10, 0);

        ArchiveDto.CreateRequest req = ArchiveDto.CreateRequest.builder()
                .title("New Archive")
                .description("Description")
                .videoUrl("https://cdn.example.com/new.mp4")
                .thumbnailUrl("https://cdn.example.com/new-thumb.jpg")
                .duration(3600)
                .startTime(start)
                .endTime(start.plusHours(1))
                .eventId("ev-2")
                .build();

        assertEquals("New Archive", req.getTitle());
        assertEquals(3600, req.getDuration());
        assertEquals("ev-2", req.getEventId());
    }

    @Test
    void archiveDto_UpdateRequest_PartialUpdate_Works() {
        ArchiveDto.UpdateRequest req = ArchiveDto.UpdateRequest.builder()
                .title("Updated Title")
                .description("Updated desc")
                .build();

        assertEquals("Updated Title", req.getTitle());
        assertEquals("Updated desc", req.getDescription());
        assertNull(req.getVideoUrl());
        assertNull(req.getDuration());
    }
}
