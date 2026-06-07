package com.platform.model.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveDtoTest {

    @Test
    void archiveDto_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        ArchiveDto dto = ArchiveDto.builder()
                .id("arch-1")
                .title("Archived Stream")
                .description("Past broadcast")
                .videoUrl("/videos/stream.mp4")
                .thumbnailUrl("/thumbs/stream.jpg")
                .duration(3600)
                .viewsCount(150)
                .startTime(now.minusHours(2))
                .endTime(now.minusHours(1))
                .eventId("evt-1")
                .createdAt(now)
                .build();

        assertEquals("arch-1", dto.getId());
        assertEquals("Archived Stream", dto.getTitle());
        assertEquals("Past broadcast", dto.getDescription());
        assertEquals("/videos/stream.mp4", dto.getVideoUrl());
        assertEquals("/thumbs/stream.jpg", dto.getThumbnailUrl());
        assertEquals(3600, dto.getDuration());
        assertEquals(150, dto.getViewsCount());
        assertEquals("evt-1", dto.getEventId());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    void createRequest_Builder_SetsAllFields() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 1, 12, 0);

        ArchiveDto.CreateRequest req = ArchiveDto.CreateRequest.builder()
                .title("New Archive")
                .description("Description")
                .videoUrl("/v.mp4")
                .thumbnailUrl("/t.jpg")
                .duration(7200)
                .startTime(start)
                .endTime(end)
                .eventId("evt-2")
                .build();

        assertEquals("New Archive", req.getTitle());
        assertEquals("Description", req.getDescription());
        assertEquals("/v.mp4", req.getVideoUrl());
        assertEquals("/t.jpg", req.getThumbnailUrl());
        assertEquals(7200, req.getDuration());
        assertEquals(start, req.getStartTime());
        assertEquals(end, req.getEndTime());
        assertEquals("evt-2", req.getEventId());
    }

    @Test
    void updateRequest_Builder_SetsAllFields() {
        ArchiveDto.UpdateRequest req = ArchiveDto.UpdateRequest.builder()
                .title("Updated Title")
                .description("Updated desc")
                .videoUrl("/new.mp4")
                .thumbnailUrl("/new.jpg")
                .duration(1800)
                .eventId("evt-3")
                .build();

        assertEquals("Updated Title", req.getTitle());
        assertEquals("Updated desc", req.getDescription());
        assertEquals("/new.mp4", req.getVideoUrl());
        assertEquals("/new.jpg", req.getThumbnailUrl());
        assertEquals(1800, req.getDuration());
        assertEquals("evt-3", req.getEventId());
    }

    @Test
    void archiveDto_NoArgsConstructor_CreatesEmptyObject() {
        ArchiveDto dto = new ArchiveDto();
        assertNull(dto.getId());
        assertNull(dto.getTitle());
        assertNull(dto.getDuration());
    }
}
