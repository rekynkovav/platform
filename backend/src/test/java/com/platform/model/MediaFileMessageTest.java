package com.platform.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MediaFileMessageTest {

    // ==================== MediaFile ====================

    @Test
    void mediaFile_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        MediaFile file = MediaFile.builder()
                .id("mf-1")
                .ownerId("user-1")
                .s3Key("uploads/video.mp4")
                .encryptionIv("iv-abc123")
                .fileType("video")
                .originalFilename("clip.mp4")
                .contentType("video/mp4")
                .fileSize(1024L)
                .duration(120)
                .watermarkData("wm-data")
                .hlsPlaylistUrl("/hls/clip.m3u8")
                .accessLevel("public")
                .viewsCount(50)
                .downloadCount(10)
                .createdAt(now)
                .updatedAt(now.plusMinutes(1))
                .build();

        assertEquals("mf-1", file.getId());
        assertEquals("uploads/video.mp4", file.getS3Key());
        assertEquals("iv-abc123", file.getEncryptionIv());
        assertEquals(1024L, file.getFileSize());
        assertEquals(120, file.getDuration());
        assertEquals("public", file.getAccessLevel());
    }

    @Test
    void mediaFile_OnPrePersist_SetsDefaults() {
        MediaFile file = new MediaFile();
        file.setOwnerId("u1");
        file.setS3Key("key");
        file.setEncryptionIv("iv");
        file.setFileType("image");
        file.setOriginalFilename("img.jpg");

        file.onCreate();

        assertNotNull(file.getCreatedAt());
        assertNotNull(file.getUpdatedAt());
        assertEquals(0, file.getViewsCount());
        assertEquals(0, file.getDownloadCount());
    }

    @Test
    void mediaFile_OnUpdate_SetsUpdatedAt() {
        MediaFile file = MediaFile.builder().updatedAt(LocalDateTime.of(2020, 1, 1, 0, 0)).build();
        file.onUpdate();
        assertNotNull(file.getUpdatedAt());
        assertTrue(file.getUpdatedAt().isAfter(LocalDateTime.of(2020, 1, 1, 0, 0)));
    }

    // ==================== Message ====================

    @Test
    void message_Builder_SetsAllFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        Message msg = Message.builder()
                .id(id)
                .content("Hello!")
                .userId(userId)
                .userName("Alice")
                .chatType("general")
                .recipientId(null)
                .isRead(false)
                .mediaId("med-1")
                .mediaType("image")
                .fileName("photo.jpg")
                .createdAt(LocalDateTime.now())
                .roomId(roomId)
                .roomSlug("general")
                .version(1L)
                .build();

        assertEquals(id, msg.getId());
        assertEquals("Hello!", msg.getContent());
        assertEquals(userId, msg.getUserId());
        assertFalse(msg.getIsRead());
        assertEquals("general", msg.getRoomSlug());
        assertEquals(1L, msg.getVersion());
    }

    @Test
    void message_NoArgsConstructor_CreatesWithDefaults() {
        Message msg = new Message();
        assertNull(msg.getId());
        assertNull(msg.getContent());
        // isRead has default false in field declaration
    }
}
