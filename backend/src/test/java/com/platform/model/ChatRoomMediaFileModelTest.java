package com.platform.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChatRoomMediaFileModelTest {

    // ==================== ChatRoom ====================

    @Test
    void chatRoom_Builder_SetsAllFields() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        ChatRoom room = ChatRoom.builder()
                .id(id)
                .slug("questions-general")
                .type("question")
                .name("General Questions")
                .description("Ask anything")
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .version(1L)
                .build();

        assertEquals(id, room.getId());
        assertEquals("questions-general", room.getSlug());
        assertEquals("question", room.getType());
        assertEquals("General Questions", room.getName());
        assertEquals("Ask anything", room.getDescription());
        assertTrue(room.getIsActive());
        assertEquals(now, room.getCreatedAt());
        assertEquals(1L, room.getVersion());
    }

    @Test
    void chatRoom_DefaultConstructor_CreatesInstance() {
        ChatRoom room = new ChatRoom();
        assertNull(room.getId());
        assertNull(room.getSlug());
    }

    @Test
    void chatRoom_SettersAndGettersWork() {
        ChatRoom room = new ChatRoom();
        room.setSlug("nutrition");
        room.setType("general");
        room.setName("Nutrition Chat");
        room.setIsActive(false);

        assertEquals("nutrition", room.getSlug());
        assertEquals("general", room.getType());
        assertEquals("Nutrition Chat", room.getName());
        assertFalse(room.getIsActive());
    }

    // ==================== MediaFile ====================

    @Test
    void mediaFile_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();

        MediaFile file = MediaFile.builder()
                .id("media-1")
                .ownerId("user-1")
                .s3Key("uploads/video.mp4")
                .encryptionIv("iv-abc123")
                .fileType("video")
                .originalFilename("demo.mp4")
                .contentType("video/mp4")
                .fileSize(1048576L)
                .duration(120)
                .watermarkData("wm-data")
                .hlsPlaylistUrl("https://cdn.example.com/playlist.m3u8")
                .accessLevel("USER_1")
                .viewsCount(50)
                .downloadCount(5)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals("media-1", file.getId());
        assertEquals("user-1", file.getOwnerId());
        assertEquals("uploads/video.mp4", file.getS3Key());
        assertEquals("iv-abc123", file.getEncryptionIv());
        assertEquals("video", file.getFileType());
        assertEquals("demo.mp4", file.getOriginalFilename());
        assertEquals("video/mp4", file.getContentType());
        assertEquals(1048576L, file.getFileSize());
        assertEquals(120, file.getDuration());
        assertEquals("wm-data", file.getWatermarkData());
        assertEquals("https://cdn.example.com/playlist.m3u8", file.getHlsPlaylistUrl());
        assertEquals("USER_1", file.getAccessLevel());
        assertEquals(50, file.getViewsCount());
        assertEquals(5, file.getDownloadCount());
    }

    @Test
    void mediaFile_OnCreate_InitializesDefaults() {
        MediaFile file = new MediaFile();
        file.setId("media-2");
        file.setOwnerId("user-2");
        file.setS3Key("key");
        file.setEncryptionIv("iv");
        file.setFileType("image");
        file.setOriginalFilename("pic.jpg");

        file.onCreate();

        assertNotNull(file.getCreatedAt());
        assertNotNull(file.getUpdatedAt());
        assertEquals(0, file.getViewsCount());
        assertEquals(0, file.getDownloadCount());
    }

    @Test
    void mediaFile_OnUpdate_UpdatesTimestamp() {
        MediaFile file = new MediaFile();
        file.onCreate();
        LocalDateTime firstUpdate = file.getUpdatedAt();

        // Small delay to ensure different timestamp
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        file.onUpdate();

        assertNotNull(file.getUpdatedAt());
        assertFalse(file.getUpdatedAt().isBefore(firstUpdate));
    }
}
