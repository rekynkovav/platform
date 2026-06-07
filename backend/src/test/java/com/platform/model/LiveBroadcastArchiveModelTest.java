package com.platform.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LiveBroadcastArchiveModelTest {

    // ==================== LiveBroadcast ====================

    @Test
    void liveBroadcast_Builder_SetsAllFields() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        LiveBroadcast broadcast = LiveBroadcast.builder()
                .id(id)
                .title("Live Q&A")
                .description("Ask questions live")
                .streamType("RTMP")
                .ownerId(ownerId)
                .status(BroadcastStatus.LIVE)
                .viewerCount(150)
                .maxViewers(200)
                .startedAt(now.minusHours(1))
                .endedAt(null)
                .hlsPlaylistUrl("https://cdn.example.com/live.m3u8")
                .rtmpStreamKey("key-abc")
                .recordedUrl("https://cdn.example.com/rec.mp4")
                .recordedAt(now)
                .recordedSize(524288000L)
                .recordedDuration(3600)
                .isPublic(true)
                .isChatEnabled(true)
                .isReactionsEnabled(true)
                .isRecording(true)
                .createdAt(now)
                .version(1L)
                .build();

        assertEquals(id, broadcast.getId());
        assertEquals("Live Q&A", broadcast.getTitle());
        assertEquals("RTMP", broadcast.getStreamType());
        assertEquals(ownerId, broadcast.getOwnerId());
        assertEquals(BroadcastStatus.LIVE, broadcast.getStatus());
        assertEquals(150, broadcast.getViewerCount());
        assertEquals(200, broadcast.getMaxViewers());
        assertTrue(broadcast.getIsPublic());
        assertTrue(broadcast.getIsChatEnabled());
        assertTrue(broadcast.getIsReactionsEnabled());
        assertTrue(broadcast.getIsRecording());
        assertEquals(524288000L, broadcast.getRecordedSize());
        assertEquals(3600, broadcast.getRecordedDuration());
    }

    @Test
    void liveBroadcast_OnCreate_InitializesDefaults() {
        LiveBroadcast broadcast = new LiveBroadcast();
        broadcast.setTitle("New Stream");
        broadcast.setStreamType("WEBRTC");
        broadcast.setOwnerId(UUID.randomUUID());

        broadcast.onCreate();

        assertNotNull(broadcast.getCreatedAt());
        assertEquals(0, broadcast.getViewerCount());
        assertEquals(0, broadcast.getMaxViewers());
        assertTrue(broadcast.getIsPublic());
        assertTrue(broadcast.getIsChatEnabled());
        assertTrue(broadcast.getIsReactionsEnabled());
        assertFalse(broadcast.getIsRecording());
        assertEquals(BroadcastStatus.IDLE, broadcast.getStatus());
    }

    @Test
    void liveBroadcast_DefaultConstructor_CreatesInstance() {
        LiveBroadcast broadcast = new LiveBroadcast();
        assertNull(broadcast.getId());
        assertNull(broadcast.getTitle());
        assertEquals(0L, broadcast.getVersion());
    }

    // ==================== Archive ====================

    @Test
    void archive_Builder_SetsAllFields() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 1, 12, 0);

        Archive archive = Archive.builder()
                .id("archive-1")
                .title("Workshop Recording")
                .description("Full workshop video")
                .videoUrl("https://cdn.example.com/workshop.mp4")
                .thumbnailUrl("https://cdn.example.com/thumb.jpg")
                .duration(7200)
                .viewsCount(300)
                .startTime(start)
                .endTime(end)
                .eventId("event-1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        assertEquals("archive-1", archive.getId());
        assertEquals("Workshop Recording", archive.getTitle());
        assertEquals("https://cdn.example.com/workshop.mp4", archive.getVideoUrl());
        assertEquals(7200, archive.getDuration());
        assertEquals(300, archive.getViewsCount());
        assertEquals(start, archive.getStartTime());
        assertEquals(end, archive.getEndTime());
        assertEquals("event-1", archive.getEventId());
    }

    @Test
    void archive_DefaultConstructor_CreatesInstance() {
        Archive archive = new Archive();
        assertNull(archive.getId());
        assertNull(archive.getTitle());
        assertNull(archive.getVideoUrl());
    }

    @Test
    void archive_SettersAndGettersWork() {
        Archive archive = new Archive();
        archive.setTitle("Updated Archive");
        archive.setViewsCount(500);
        archive.setDuration(1800);

        assertEquals("Updated Archive", archive.getTitle());
        assertEquals(500, archive.getViewsCount());
        assertEquals(1800, archive.getDuration());
    }
}
