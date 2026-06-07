package com.platform.model.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MediaFileMessageDtoTest {

    // ==================== MediaFileDto ====================

    @Test
    void mediaFileDto_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        MediaFileDto dto = MediaFileDto.builder()
                .id("mf-1")
                .ownerId("user-1")
                .fileType("video")
                .originalFilename("clip.mp4")
                .contentType("video/mp4")
                .fileSize(1024L)
                .duration(120)
                .accessLevel("public")
                .thumbnailUrl("/thumbs/clip.jpg")
                .viewsCount(50)
                .downloadCount(10)
                .createdAt(now)
                .updatedAt(now.plusMinutes(1))
                .build();

        assertEquals("mf-1", dto.getId());
        assertEquals("video", dto.getFileType());
        assertEquals(1024L, dto.getFileSize());
        assertEquals(120, dto.getDuration());
        assertEquals("public", dto.getAccessLevel());
        assertEquals(50, dto.getViewsCount());
    }

    @Test
    void mediaFileDto_UploadResponse_BuilderSetsFields() {
        MediaFileDto.UploadResponse resp = MediaFileDto.UploadResponse.builder()
                .fileId("mf-2")
                .url("/uploads/file.mp4")
                .type("video")
                .size(2048L)
                .message("Uploaded successfully")
                .build();

        assertEquals("mf-2", resp.getFileId());
        assertEquals("/uploads/file.mp4", resp.getUrl());
        assertEquals(2048L, resp.getSize());
        assertEquals("Uploaded successfully", resp.getMessage());
    }

    @Test
    void mediaFileDto_StreamResponse_BuilderSetsFields() {
        MediaFileDto.StreamResponse resp = MediaFileDto.StreamResponse.builder()
                .url("/stream/token123")
                .expiresIn(3600)
                .type("hls")
                .build();

        assertEquals("/stream/token123", resp.getUrl());
        assertEquals(3600, resp.getExpiresIn());
        assertEquals("hls", resp.getType());
    }

    // ==================== MessageDto ====================

    @Test
    void messageDto_Builder_SetsAllFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        MessageDto dto = MessageDto.builder()
                .id(id)
                .type("text")
                .content("Hello!")
                .userId(userId)
                .userName("Alice")
                .chatType("private")
                .recipientId(UUID.randomUUID())
                .isRead(false)
                .createdAt(now)
                .mediaId("med-1")
                .mediaType("image")
                .fileName("photo.jpg")
                .mediaUrl("/media/photo.jpg")
                .roomId(roomId)
                .roomSlug("general")
                .build();

        assertEquals(id, dto.getId());
        assertEquals("Hello!", dto.getContent());
        assertEquals(userId, dto.getUserId());
        assertFalse(dto.getIsRead());
        assertEquals("general", dto.getRoomSlug());
    }

    @Test
    void messageDto_SendRequest_BuilderSetsFields() {
        UUID recipient = UUID.randomUUID();
        MessageDto.SendRequest req = MessageDto.SendRequest.builder()
                .content("Hi there")
                .recipientId(recipient)
                .chatType("direct")
                .mediaId("med-2")
                .build();

        assertEquals("Hi there", req.getContent());
        assertEquals(recipient, req.getRecipientId());
        assertEquals("direct", req.getChatType());
    }

    @Test
    void messageDto_ProjectionDto_ConstructorSetsFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        MessageDto.ProjectionDto proj = new MessageDto.ProjectionDto(id, "Projected msg", now, "Bob");

        assertEquals(id, proj.getId());
        assertEquals("Projected msg", proj.getContent());
        assertEquals(now, proj.getCreatedAt());
        assertEquals("Bob", proj.getAuthorName());
    }
}
