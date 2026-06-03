package com.platform.model.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ChatRoomCommentDtoTest {

    // ==================== ChatRoomDto ====================

    @Test
    void chatRoomDto_Builder_SetsAllFields() {
        ChatRoomDto dto = ChatRoomDto.builder()
                .id(1L)
                .slug("general")
                .type("public")
                .name("General Chat")
                .description("Main chat room")
                .isActive(true)
                .build();

        assertEquals(1L, dto.getId());
        assertEquals("general", dto.getSlug());
        assertEquals("public", dto.getType());
        assertEquals("General Chat", dto.getName());
        assertEquals("Main chat room", dto.getDescription());
        assertTrue(dto.getIsActive());
    }

    @Test
    void chatRoomDto_NoArgsConstructor_CreatesEmptyObject() {
        ChatRoomDto dto = new ChatRoomDto();
        assertNull(dto.getId());
        assertNull(dto.getSlug());
        assertNull(dto.getIsActive());
    }

    // ==================== CommentDto ====================

    @Test
    void commentDto_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        CommentDto dto = CommentDto.builder()
                .id("cmt-1")
                .content("Great post!")
                .postId("post-1")
                .authorId("user-1")
                .authorName("Alice")
                .createdAt(now)
                .canDelete(true)
                .build();

        assertEquals("cmt-1", dto.getId());
        assertEquals("Great post!", dto.getContent());
        assertEquals("post-1", dto.getPostId());
        assertEquals("user-1", dto.getAuthorId());
        assertEquals("Alice", dto.getAuthorName());
        assertEquals(now, dto.getCreatedAt());
        assertTrue(dto.getCanDelete());
    }

    @Test
    void commentDto_CreateRequest_BuilderSetsContent() {
        CommentDto.CreateRequest req = CommentDto.CreateRequest.builder()
                .content("New comment")
                .build();

        assertEquals("New comment", req.getContent());
    }

    @Test
    void commentDto_NoArgsConstructor_CreatesEmptyObject() {
        CommentDto dto = new CommentDto();
        assertNull(dto.getId());
        assertNull(dto.getContent());
        assertNull(dto.getCanDelete());
    }
}
