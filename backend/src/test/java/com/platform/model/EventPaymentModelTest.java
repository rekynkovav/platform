package com.platform.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventPaymentModelTest {

    // ==================== Event ====================

    @Test
    void event_Builder_SetsAllFields() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 15, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 15, 12, 0);
        Map<String, String> links = Map.of("zoom", "https://zoom.us/j/123", "telegram", "https://t.me/event");

        Event event = Event.builder()
                .id("event-1")
                .title("Summer Workshop")
                .description("A great workshop")
                .startTime(start)
                .endTime(end)
                .coverImage("https://example.com/cover.jpg")
                .links(links)
                .createdBy("admin-1")
                .createdAt(LocalDateTime.now())
                .streamUrl("rtmp://stream.example.com/live")
                .status("upcoming")
                .build();

        assertEquals("event-1", event.getId());
        assertEquals("Summer Workshop", event.getTitle());
        assertEquals("A great workshop", event.getDescription());
        assertEquals(start, event.getStartTime());
        assertEquals(end, event.getEndTime());
        assertEquals("https://example.com/cover.jpg", event.getCoverImage());
        assertEquals(2, event.getLinks().size());
        assertEquals("admin-1", event.getCreatedBy());
        assertEquals("rtmp://stream.example.com/live", event.getStreamUrl());
        assertEquals("upcoming", event.getStatus());
    }

    @Test
    void event_DefaultConstructor_CreatesInstance() {
        Event event = new Event();
        assertNull(event.getId());
        assertNull(event.getTitle());
        assertNull(event.getLinks());
    }

    @Test
    void event_SettersAndGettersWork() {
        Event event = new Event();
        event.setTitle("Updated Event");
        event.setStatus("live");
        event.setStreamUrl("rtmp://new.stream/url");

        assertEquals("Updated Event", event.getTitle());
        assertEquals("live", event.getStatus());
        assertEquals("rtmp://new.stream/url", event.getStreamUrl());
    }

    // ==================== Payment ====================

    @Test
    void payment_Builder_SetsAllFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Payment payment = Payment.builder()
                .id(id)
                .userId(userId)
                .amount(new BigDecimal("999.00"))
                .currency("RUB")
                .subscriptionMonths(1)
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod("SBP")
                .bankTransactionId("txn-123")
                .qrCodeData("qr-data-here")
                .expiresAt(now.plusHours(24))
                .paidAt(null)
                .confirmedBy(null)
                .notes("Awaiting confirmation")
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();

        assertEquals(id, payment.getId());
        assertEquals(userId, payment.getUserId());
        assertEquals(new BigDecimal("999.00"), payment.getAmount());
        assertEquals("RUB", payment.getCurrency());
        assertEquals(1, payment.getSubscriptionMonths());
        assertEquals(Payment.PaymentStatus.PENDING, payment.getStatus());
        assertEquals("SBP", payment.getPaymentMethod());
        assertEquals("txn-123", payment.getBankTransactionId());
        assertEquals("qr-data-here", payment.getQrCodeData());
        assertEquals("Awaiting confirmation", payment.getNotes());
    }

    @Test
    void payment_DefaultValues_AreCorrect() {
        Payment payment = new Payment();
        assertEquals("RUB", payment.getCurrency());
        assertEquals(Payment.PaymentStatus.PENDING, payment.getStatus());
        assertEquals(0L, payment.getVersion());
    }

    @Test
    void payment_PaymentStatus_AllValuesExist() {
        Payment.PaymentStatus[] statuses = Payment.PaymentStatus.values();
        assertEquals(6, statuses.length);
        assertNotNull(Payment.PaymentStatus.valueOf("PENDING"));
        assertNotNull(Payment.PaymentStatus.valueOf("PAID"));
        assertNotNull(Payment.PaymentStatus.valueOf("CONFIRMED"));
        assertNotNull(Payment.PaymentStatus.valueOf("EXPIRED"));
        assertNotNull(Payment.PaymentStatus.valueOf("CANCELLED"));
        assertNotNull(Payment.PaymentStatus.valueOf("REFUNDED"));
    }
}
