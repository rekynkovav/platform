package com.platform.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventMaterialLiveBroadcastTest {

    // ==================== EventMaterial ====================

    @Test
    void eventMaterial_Builder_SetsAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        EventMaterial material = EventMaterial.builder()
                .id(id)
                .sectionId("about")
                .type("text")
                .title("About Us")
                .content("We are a platform...")
                .url("/pages/about")
                .mediaId("med-1")
                .createdBy("admin")
                .authorName("Admin")
                .sortOrder(1)
                .isActive(true)
                .createdAt(now)
                .updatedAt(now.plusMinutes(5))
                .build();

        assertEquals(id, material.getId());
        assertEquals("about", material.getSectionId());
        assertEquals("text", material.getType());
        assertEquals("About Us", material.getTitle());
        assertEquals(1, material.getSortOrder());
        assertTrue(material.getIsActive());
    }

    @Test
    void eventMaterial_NoArgsConstructor_CreatesEmptyObject() {
        EventMaterial material = new EventMaterial();
        assertNull(material.getId());
        assertNull(material.getSectionId());
        assertNull(material.getTitle());
    }

    // ==================== LiveBroadcast ====================

    @Test
    void liveBroadcast_Builder_SetsAllFields() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        LiveBroadcast broadcast = LiveBroadcast.builder()
                .id(id)
                .title("Live Show")
                .description("Streaming now")
                .streamType("hls")
                .ownerId(ownerId)
                .status(BroadcastStatus.LIVE)
                .viewerCount(100)
                .maxViewers(150)
                .startedAt(LocalDateTime.now())
                .hlsPlaylistUrl("/hls/playlist.m3u8")
                .isPublic(true)
                .isChatEnabled(true)
                .isReactionsEnabled(true)
                .isRecording(false)
                .version(1L)
                .build();

        assertEquals(id, broadcast.getId());
        assertEquals("Live Show", broadcast.getTitle());
        assertEquals(BroadcastStatus.LIVE, broadcast.getStatus());
        assertEquals(100, broadcast.getViewerCount());
        assertTrue(broadcast.getIsPublic());
    }

    @Test
    void liveBroadcast_OnPrePersist_SetsDefaults() {
        LiveBroadcast broadcast = new LiveBroadcast();
        broadcast.setTitle("New Stream");
        broadcast.setStreamType("rtmp");
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
    void liveBroadcast_OnPrePersist_DoesNotOverrideExistingStatus() {
        LiveBroadcast broadcast = new LiveBroadcast();
        broadcast.setTitle("Live");
        broadcast.setStreamType("hls");
        broadcast.setOwnerId(UUID.randomUUID());
        broadcast.setStatus(BroadcastStatus.LIVE);

        broadcast.onCreate();

        assertEquals(BroadcastStatus.LIVE, broadcast.getStatus());
    }
}
