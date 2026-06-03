package com.platform.model.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventMaterialLiveBroadcastDtoTest {

    // ==================== EventMaterialDto ====================

    @Test
    void eventMaterialDto_Builder_SetsAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        EventMaterialDto dto = EventMaterialDto.builder()
                .id(id)
                .sectionId("sec-1")
                .type("document")
                .title("Presentation")
                .content("Slide content")
                .url("/files/pres.pdf")
                .mediaId("med-1")
                .createdBy("user-1")
                .authorName("Alice")
                .sortOrder(1)
                .isActive(true)
                .createdAt(now)
                .updatedAt(now.plusMinutes(5))
                .build();

        assertEquals(id, dto.getId());
        assertEquals("sec-1", dto.getSectionId());
        assertEquals("document", dto.getType());
        assertEquals("Presentation", dto.getTitle());
        assertEquals("/files/pres.pdf", dto.getUrl());
        assertEquals(1, dto.getSortOrder());
        assertTrue(dto.getIsActive());
    }

    @Test
    void eventMaterialDto_CreateRequest_SettersWork() {
        EventMaterialDto.CreateRequest req = new EventMaterialDto.CreateRequest();
        req.setSectionId("sec-2");
        req.setType("video");
        req.setTitle("Intro Video");
        req.setContent("Video description");
        req.setUrl("/videos/intro.mp4");
        req.setMediaId("med-2");
        req.setSortOrder(0);

        assertEquals("sec-2", req.getSectionId());
        assertEquals("video", req.getType());
        assertEquals("Intro Video", req.getTitle());
        assertEquals(0, req.getSortOrder());
    }

    @Test
    void eventMaterialDto_UpdateRequest_SettersWork() {
        EventMaterialDto.UpdateRequest req = new EventMaterialDto.UpdateRequest();
        req.setTitle("Updated Title");
        req.setContent("Updated content");
        req.setIsActive(false);
        req.setSortOrder(5);

        assertEquals("Updated Title", req.getTitle());
        assertFalse(req.getIsActive());
        assertEquals(5, req.getSortOrder());
    }

    // ==================== LiveBroadcastDto ====================

    @Test
    void liveBroadcastDto_SettersAndGetters_Work() {
        LiveBroadcastDto dto = new LiveBroadcastDto();
        LocalDateTime now = LocalDateTime.now();

        dto.setId("lb-1");
        dto.setTitle("Live Stream");
        dto.setDescription("Streaming now");
        dto.setStreamType("hls");
        dto.setUserId("user-1");
        dto.setStatus("live");
        dto.setCreatedAt(now);
        dto.setStartedAt(now);
        dto.setEndedAt(null);
        dto.setViewerCount(42);
        dto.setHlsPlaylistUrl("/hls/playlist.m3u8");
        dto.setThumbnailUrl("/thumbs/live.jpg");

        assertEquals("lb-1", dto.getId());
        assertEquals("Live Stream", dto.getTitle());
        assertEquals("hls", dto.getStreamType());
        assertEquals("live", dto.getStatus());
        assertEquals(42, dto.getViewerCount());
        assertEquals("/hls/playlist.m3u8", dto.getHlsPlaylistUrl());
        assertNull(dto.getEndedAt());
    }

    @Test
    void liveBroadcastDto_NoArgsConstructor_CreatesEmptyObject() {
        LiveBroadcastDto dto = new LiveBroadcastDto();
        assertNull(dto.getId());
        assertNull(dto.getTitle());
        assertNull(dto.getViewerCount());
    }
}
