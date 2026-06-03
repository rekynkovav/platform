package com.platform.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationPaymentTest {

    // ==================== Notification ====================

    @Test
    void notification_Builder_SetsAllFields() {
        Instant now = Instant.now();
        Notification notif = Notification.builder()
                .id(1L)
                .userId("user-1")
                .type("event")
                .title("New Event")
                .message("A new event has been created")
                .relatedId(42L)
                .relatedRoomSlug("questions-general")
                .read(false)
                .createdAt(now)
                .build();

        assertEquals(1L, notif.getId());
        assertEquals("user-1", notif.getUserId());
        assertEquals("event", notif.getType());
        assertEquals("New Event", notif.getTitle());
        assertEquals(42L, notif.getRelatedId());
        assertFalse(notif.getRead());
    }

    @Test
    void notification_NoArgsConstructor_CreatesWithDefaults() {
        Notification notif = new Notification();
        assertNull(notif.getId());
        assertNull(notif.getUserId());
        // read has default false in field declaration
    }

    @Test
    void notification_SettersAndGetters_Work() {
        Notification notif = new Notification();
        notif.setUserId("user-2");
        notif.setType("answer");
        notif.setTitle("Answer received");
        notif.setMessage("Your question was answered");
        notif.setRead(true);

        assertEquals("user-2", notif.getUserId());
        assertEquals("answer", notif.getType());
        assertTrue(notif.getRead());
    }

    // ==================== Payment ====================

    @Test
    void payment_Builder_SetsAllFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Payment payment = Payment.builder()
                .id(id)
                .userId(userId)
                .amount(new BigDecimal("999.00"))
                .currency("RUB")
                .subscriptionMonths(3)
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod("SBP")
                .bankTransactionId("txn-123")
                .qrCodeData("qr-data")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .paidAt(null)
                .confirmedBy(null)
                .notes("Awaiting payment")
                .version(0L)
                .build();

        assertEquals(id, payment.getId());
        assertEquals(userId, payment.getUserId());
        assertEquals(new BigDecimal("999.00"), payment.getAmount());
        assertEquals(Payment.PaymentStatus.PENDING, payment.getStatus());
        assertEquals("SBP", payment.getPaymentMethod());
    }

    @Test
    void payment_PaymentStatus_EnumValues() {
        assertEquals(6, Payment.PaymentStatus.values().length);
        assertNotNull(Payment.PaymentStatus.valueOf("PAID"));
        assertNotNull(Payment.PaymentStatus.valueOf("CONFIRMED"));
        assertNotNull(Payment.PaymentStatus.valueOf("EXPIRED"));
        assertNotNull(Payment.PaymentStatus.valueOf("CANCELLED"));
        assertNotNull(Payment.PaymentStatus.valueOf("REFUNDED"));
    }
}
