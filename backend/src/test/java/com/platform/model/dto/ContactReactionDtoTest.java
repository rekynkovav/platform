package com.platform.model.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ContactReactionDtoTest {

    // ==================== ContactDto ====================

    @Test
    void contactDto_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();

        ContactDto dto = ContactDto.builder()
                .id("contact-1")
                .userId("user-1")
                .contactUserId("user-2")
                .contactName("Bob")
                .status("FRIEND")
                .avatarUrl("https://cdn.example.com/bob.jpg")
                .createdAt(now)
                .build();

        assertEquals("contact-1", dto.getId());
        assertEquals("user-1", dto.getUserId());
        assertEquals("user-2", dto.getContactUserId());
        assertEquals("Bob", dto.getContactName());
        assertEquals("FRIEND", dto.getStatus());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    void contactDto_AddRequest_BuilderWorks() {
        ContactDto.AddRequest req = ContactDto.AddRequest.builder()
                .contactUserId("user-3")
                .nickname("Charlie")
                .build();

        assertEquals("user-3", req.getContactUserId());
        assertEquals("Charlie", req.getNickname());
    }

    @Test
    void contactDto_UpdateRequest_BuilderWorks() {
        ContactDto.UpdateRequest req = ContactDto.UpdateRequest.builder()
                .nickname("New Nickname")
                .build();

        assertEquals("New Nickname", req.getNickname());
    }

    // ==================== ReactionDto ====================

    @Test
    void reactionDto_Builder_SetsAllFields() {
        ReactionDto dto = ReactionDto.builder()
                .postId("post-1")
                .userId("user-1")
                .type("like")
                .count(42L)
                .userReacted(true)
                .build();

        assertEquals("post-1", dto.getPostId());
        assertEquals("user-1", dto.getUserId());
        assertEquals("like", dto.getType());
        assertEquals(42L, dto.getCount());
        assertTrue(dto.getUserReacted());
    }

    @Test
    void reactionDto_UserNotReacted_HasFalse() {
        ReactionDto dto = ReactionDto.builder()
                .postId("post-2")
                .type("love")
                .count(10L)
                .userReacted(false)
                .build();

        assertFalse(dto.getUserReacted());
        assertNull(dto.getUserId());
    }
}
