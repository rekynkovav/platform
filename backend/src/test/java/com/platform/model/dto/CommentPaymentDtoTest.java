package com.platform.model.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CommentPaymentDtoTest {

    // ==================== CommentDto ====================

    @Test
    void commentDto_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();

        CommentDto dto = CommentDto.builder()
                .id("c1")
                .content("Great post!")
                .postId("p1")
                .authorId("u1")
                .authorName("Alice")
                .createdAt(now)
                .canDelete(true)
                .build();

        assertEquals("c1", dto.getId());
        assertEquals("Great post!", dto.getContent());
        assertEquals("p1", dto.getPostId());
        assertEquals("u1", dto.getAuthorId());
        assertEquals("Alice", dto.getAuthorName());
        assertEquals(now, dto.getCreatedAt());
        assertTrue(dto.getCanDelete());
    }

    @Test
    void commentDto_CreateRequest_BuilderWorks() {
        CommentDto.CreateRequest req = CommentDto.CreateRequest.builder()
                .content("New comment")
                .build();

        assertEquals("New comment", req.getContent());
    }

    @Test
    void commentDto_DefaultValues_AreNull() {
        CommentDto dto = new CommentDto();
        assertNull(dto.getId());
        assertNull(dto.getContent());
        assertNull(dto.getCanDelete());
    }

    // ==================== PaymentDto ====================

    @Test
    void paymentDto_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = now.plusMinutes(30);

        PaymentDto dto = PaymentDto.builder()
                .id("pay-1")
                .userId("user-1")
                .amount(new BigDecimal("999.00"))
                .currency("RUB")
                .subscriptionMonths(3)
                .status("PENDING")
                .paymentMethod("SBP")
                .qrCodeData("https://qr.nspk.ru/abc123")
                .qrCodeBase64("iVBORw0KGgo...")
                .expiresAt(expires)
                .paidAt(null)
                .createdAt(now)
                .build();

        assertEquals("pay-1", dto.getId());
        assertEquals("user-1", dto.getUserId());
        assertEquals(new BigDecimal("999.00"), dto.getAmount());
        assertEquals("RUB", dto.getCurrency());
        assertEquals(3, dto.getSubscriptionMonths());
        assertEquals("PENDING", dto.getStatus());
        assertEquals("SBP", dto.getPaymentMethod());
        assertNotNull(dto.getQrCodeData());
        assertNull(dto.getPaidAt());
        assertEquals(expires, dto.getExpiresAt());
    }

    @Test
    void paymentDto_CreateRequest_BuilderWorks() {
        PaymentDto.CreateRequest req = new PaymentDto.CreateRequest(12);
        assertEquals(12, req.getSubscriptionMonths());
    }

    @Test
    void paymentDto_ConfirmRequest_BuilderWorks() {
        PaymentDto.ConfirmRequest req = new PaymentDto.ConfirmRequest("Payment verified manually");
        assertEquals("Payment verified manually", req.getNotes());
    }
}
