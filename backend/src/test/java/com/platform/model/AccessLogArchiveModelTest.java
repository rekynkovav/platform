package com.platform.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AccessLogArchiveModelTest {

    // ==================== AccessLog ====================

    @Test
    void accessLog_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        AccessLog log = AccessLog.builder()
                .id(1L)
                .fileId("file-1")
                .userId("user-1")
                .action("download")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .success(true)
                .errorMessage(null)
                .timestamp(now)
                .build();

        assertEquals(1L, log.getId());
        assertEquals("file-1", log.getFileId());
        assertEquals("download", log.getAction());
        assertTrue(log.isSuccess());
        assertNull(log.getErrorMessage());
        assertEquals(now, log.getTimestamp());
    }

    @Test
    void accessLog_OnCreate_SetsTimestamp() {
        AccessLog log = new AccessLog();
        assertNull(log.getTimestamp());

        log.onCreate();

        assertNotNull(log.getTimestamp());
    }

    @Test
    void accessLog_NoArgsConstructor_CreatesEmptyObject() {
        AccessLog log = new AccessLog();
        assertNull(log.getId());
        assertNull(log.getFileId());
        assertFalse(log.isSuccess());
    }

    // ==================== Archive ====================

    @Test
    void archive_Builder_SetsAllFields() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 1, 12, 0);
        LocalDateTime now = LocalDateTime.now();

        Archive archive = Archive.builder()
                .id("arch-1")
                .title("Past Stream")
                .description("Recorded broadcast")
                .videoUrl("/videos/stream.mp4")
                .thumbnailUrl("/thumbs/stream.jpg")
                .duration(7200)
                .viewsCount(200)
                .startTime(start)
                .endTime(end)
                .eventId("evt-1")
                .createdAt(now)
                .updatedAt(now.plusMinutes(5))
                .build();

        assertEquals("arch-1", archive.getId());
        assertEquals("Past Stream", archive.getTitle());
        assertEquals(7200, archive.getDuration());
        assertEquals(200, archive.getViewsCount());
        assertEquals(start, archive.getStartTime());
        assertEquals(end, archive.getEndTime());
    }

    @Test
    void archive_NoArgsConstructor_CreatesEmptyObject() {
        Archive archive = new Archive();
        assertNull(archive.getId());
        assertNull(archive.getTitle());
        assertNull(archive.getDuration());
    }
}
