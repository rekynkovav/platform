package com.platform.model.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentPostDtoTest {

    // ==================== PaymentDto ====================

    @Test
    void paymentDto_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        PaymentDto dto = PaymentDto.builder()
                .id("pay-1")
                .userId("user-1")
                .amount(new BigDecimal("99.99"))
                .currency("RUB")
                .subscriptionMonths(3)
                .status("pending")
                .paymentMethod("qr")
                .qrCodeData("qr-data-string")
                .qrCodeBase64("base64encoded")
                .expiresAt(now.plusHours(1))
                .paidAt(null)
                .createdAt(now)
                .build();

        assertEquals("pay-1", dto.getId());
        assertEquals(new BigDecimal("99.99"), dto.getAmount());
        assertEquals("RUB", dto.getCurrency());
        assertEquals(3, dto.getSubscriptionMonths());
        assertEquals("pending", dto.getStatus());
        assertNull(dto.getPaidAt());
    }

    @Test
    void paymentDto_CreateRequest_SettersWork() {
        PaymentDto.CreateRequest req = new PaymentDto.CreateRequest(12);
        assertEquals(12, req.getSubscriptionMonths());

        req.setSubscriptionMonths(6);
        assertEquals(6, req.getSubscriptionMonths());
    }

    @Test
    void paymentDto_ConfirmRequest_SettersWork() {
        PaymentDto.ConfirmRequest req = new PaymentDto.ConfirmRequest("Payment confirmed");
        assertEquals("Payment confirmed", req.getNotes());

        req.setNotes("Updated notes");
        assertEquals("Updated notes", req.getNotes());
    }

    // ==================== PostDto ====================

    @Test
    void postDto_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        CommentDto comment = CommentDto.builder().id("c1").content("Nice!").build();

        PostDto dto = PostDto.builder()
                .id("post-1")
                .content("Hello world")
                .authorId("user-1")
                .authorName("Alice")
                .authorRole("USER_1")
                .mediaUrl("/media/img.jpg")
                .mediaType("image")
                .category("general")
                .likesCount(10)
                .commentsCount(5)
                .sharesCount(2)
                .viewsCount(100)
                .userLiked(true)
                .isPinned(false)
                .createdAt(now)
                .updatedAt(now.plusMinutes(5))
                .recentComments(List.of(comment))
                .build();

        assertEquals("post-1", dto.getId());
        assertEquals("Hello world", dto.getContent());
        assertEquals(10, dto.getLikesCount());
        assertTrue(dto.getUserLiked());
        assertFalse(dto.getIsPinned());
        assertEquals(1, dto.getRecentComments().size());
    }

    @Test
    void postDto_CreateRequest_BuilderSetsFields() {
        PostDto.CreateRequest req = PostDto.CreateRequest.builder()
                .content("New post")
                .mediaUrl("/m/video.mp4")
                .mediaType("video")
                .category("news")
                .build();

        assertEquals("New post", req.getContent());
        assertEquals("video", req.getMediaType());
        assertEquals("news", req.getCategory());
    }

    @Test
    void postDto_UpdateRequest_BuilderSetsFields() {
        PostDto.UpdateRequest req = PostDto.UpdateRequest.builder()
                .content("Edited")
                .mediaUrl("/m/new.jpg")
                .mediaType("image")
                .build();

        assertEquals("Edited", req.getContent());
        assertEquals("/m/new.jpg", req.getMediaUrl());
    }

    @Test
    void postDto_MediaUploadResponse_BuilderSetsFields() {
        PostDto.MediaUploadResponse resp = PostDto.MediaUploadResponse.builder()
                .url("/uploads/file.png")
                .type("image/png")
                .size(4096L)
                .filename("file.png")
                .build();

        assertEquals("/uploads/file.png", resp.getUrl());
        assertEquals(4096L, resp.getSize());
        assertEquals("file.png", resp.getFilename());
    }
}
