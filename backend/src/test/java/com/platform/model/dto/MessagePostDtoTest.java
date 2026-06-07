package com.platform.model.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MessagePostDtoTest {

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
                .content("Hello world")
                .userId(userId)
                .userName("Alice")
                .chatType("general")
                .isRead(false)
                .createdAt(now)
                .mediaId("m1")
                .mediaType("image")
                .fileName("photo.jpg")
                .mediaUrl("https://cdn.example.com/photo.jpg")
                .roomId(roomId)
                .roomSlug("general-chat")
                .build();

        assertEquals(id, dto.getId());
        assertEquals("text", dto.getType());
        assertEquals("Hello world", dto.getContent());
        assertEquals(userId, dto.getUserId());
        assertEquals("Alice", dto.getUserName());
        assertEquals("general", dto.getChatType());
        assertFalse(dto.getIsRead());
        assertEquals("m1", dto.getMediaId());
        assertEquals("image", dto.getMediaType());
        assertEquals(roomId, dto.getRoomId());
        assertEquals("general-chat", dto.getRoomSlug());
    }

    @Test
    void messageDto_SendRequest_BuilderWorks() {
        UUID recipientId = UUID.randomUUID();

        MessageDto.SendRequest req = MessageDto.SendRequest.builder()
                .content("Private msg")
                .recipientId(recipientId)
                .chatType("private")
                .mediaId("media-123")
                .build();

        assertEquals("Private msg", req.getContent());
        assertEquals(recipientId, req.getRecipientId());
        assertEquals("private", req.getChatType());
        assertEquals("media-123", req.getMediaId());
    }

    @Test
    void messageDto_ProjectionDto_ConstructorSetsFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        MessageDto.ProjectionDto proj = new MessageDto.ProjectionDto(id, "Projected content", now, "Bob");

        assertEquals(id, proj.getId());
        assertEquals("Projected content", proj.getContent());
        assertEquals(now, proj.getCreatedAt());
        assertEquals("Bob", proj.getAuthorName());
    }

    // ==================== PostDto ====================

    @Test
    void postDto_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();

        PostDto dto = PostDto.builder()
                .id("post-1")
                .content("Test post")
                .authorId("author-1")
                .authorName("Charlie")
                .authorRole("ADMIN")
                .category("nutrition")
                .likesCount(42)
                .commentsCount(5)
                .sharesCount(3)
                .viewsCount(100)
                .userLiked(true)
                .isPinned(false)
                .createdAt(now)
                .updatedAt(now.plusMinutes(10))
                .build();

        assertEquals("post-1", dto.getId());
        assertEquals("Test post", dto.getContent());
        assertEquals("nutrition", dto.getCategory());
        assertEquals(42, dto.getLikesCount());
        assertTrue(dto.getUserLiked());
        assertFalse(dto.getIsPinned());
    }

    @Test
    void postDto_CreateRequest_BuilderWorks() {
        PostDto.CreateRequest req = PostDto.CreateRequest.builder()
                .content("New post")
                .mediaUrl("https://cdn.example.com/img.png")
                .mediaType("image")
                .category("general")
                .build();

        assertEquals("New post", req.getContent());
        assertEquals("image", req.getMediaType());
        assertEquals("general", req.getCategory());
    }

    @Test
    void postDto_MediaUploadResponse_BuilderWorks() {
        PostDto.MediaUploadResponse resp = PostDto.MediaUploadResponse.builder()
                .url("https://cdn.example.com/uploaded.mp4")
                .type("video")
                .size(1048576L)
                .filename("clip.mp4")
                .build();

        assertEquals("https://cdn.example.com/uploaded.mp4", resp.getUrl());
        assertEquals("video", resp.getType());
        assertEquals(1048576L, resp.getSize());
        assertEquals("clip.mp4", resp.getFilename());
    }
}
