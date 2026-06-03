package com.platform.model.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ChatRoomMediaFileDtoTest {

    // ==================== ChatRoomDto ====================

    @Test
    void chatRoomDto_Builder_SetsAllFields() {
        ChatRoomDto dto = ChatRoomDto.builder()
                .id(1L)
                .slug("general")
                .type("general")
                .name("General Chat")
                .description("Main discussion room")
                .isActive(true)
                .build();

        assertEquals(1L, dto.getId());
        assertEquals("general", dto.getSlug());
        assertEquals("general", dto.getType());
        assertEquals("General Chat", dto.getName());
        assertEquals("Main discussion room", dto.getDescription());
        assertTrue(dto.getIsActive());
    }

    @Test
    void chatRoomDto_InactiveRoom_HasFalseActive() {
        ChatRoomDto dto = ChatRoomDto.builder()
                .id(2L)
                .slug("archived")
                .type("general")
                .name("Old Room")
                .isActive(false)
                .build();

        assertFalse(dto.getIsActive());
    }

    // ==================== MediaFileDto ====================

    @Test
    void mediaFileDto_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();

        MediaFileDto dto = MediaFileDto.builder()
                .id("mf-1")
                .ownerId("user-1")
                .fileType("video")
                .originalFilename("presentation.mp4")
                .contentType("video/mp4")
                .fileSize(52428800L)
                .duration(300)
                .accessLevel("PUBLIC")
                .thumbnailUrl("https://cdn.example.com/thumb.jpg")
                .viewsCount(120)
                .downloadCount(15)
                .createdAt(now)
                .updatedAt(now.plusMinutes(5))
                .build();

        assertEquals("mf-1", dto.getId());
        assertEquals("video", dto.getFileType());
        assertEquals("presentation.mp4", dto.getOriginalFilename());
        assertEquals(52428800L, dto.getFileSize());
        assertEquals(300, dto.getDuration());
        assertEquals("PUBLIC", dto.getAccessLevel());
        assertEquals(120, dto.getViewsCount());
        assertEquals(15, dto.getDownloadCount());
    }

    @Test
    void mediaFileDto_UploadResponse_BuilderWorks() {
        MediaFileDto.UploadResponse resp = MediaFileDto.UploadResponse.builder()
                .fileId("uploaded-1")
                .url("https://cdn.example.com/uploaded.mp4")
                .type("video")
                .size(10485760L)
                .message("Upload successful")
                .build();

        assertEquals("uploaded-1", resp.getFileId());
        assertEquals("https://cdn.example.com/uploaded.mp4", resp.getUrl());
        assertEquals("video", resp.getType());
        assertEquals(10485760L, resp.getSize());
        assertEquals("Upload successful", resp.getMessage());
    }

    @Test
    void mediaFileDto_StreamResponse_BuilderWorks() {
        MediaFileDto.StreamResponse resp = MediaFileDto.StreamResponse.builder()
                .url("https://cdn.example.com/stream.m3u8?token=abc")
                .expiresIn(3600)
                .type("HLS")
                .build();

        assertNotNull(resp.getUrl());
        assertEquals(3600, resp.getExpiresIn());
        assertEquals("HLS", resp.getType());
    }
}
