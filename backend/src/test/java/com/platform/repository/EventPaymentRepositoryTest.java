package com.platform.repository;

import com.platform.config.TestJpaConfig;
import com.platform.model.Event;
import com.platform.model.Payment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(TestJpaConfig.class)
@ActiveProfiles("test")
class EventPaymentRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    // ==================== EventRepository ====================

    @Test
    void eventRepository_FindUpcoming_ReturnsFutureEventsOrderedAsc() {
        LocalDateTime now = LocalDateTime.now();
        eventRepository.save(Event.builder().title("Past").startTime(now.minusDays(1)).build());
        Event soon = eventRepository.save(Event.builder().title("Soon").startTime(now.plusHours(2)).build());
        Event later = eventRepository.save(Event.builder().title("Later").startTime(now.plusDays(1)).build());

        List<Event> upcoming = eventRepository.findUpcoming(now);
        assertEquals(2, upcoming.size());
        assertEquals("Soon", upcoming.get(0).getTitle());
        assertEquals("Later", upcoming.get(1).getTitle());
    }

    @Test
    void eventRepository_FindPast_ReturnsPastEventsOrderedDesc() {
        LocalDateTime now = LocalDateTime.now();
        Event old = eventRepository.save(Event.builder().title("Old").startTime(now.minusDays(3)).build());
        Event recent = eventRepository.save(Event.builder().title("Recent").startTime(now.minusDays(1)).build());
        eventRepository.save(Event.builder().title("Future").startTime(now.plusDays(1)).build());

        List<Event> past = eventRepository.findPast(now);
        assertEquals(2, past.size());
        assertEquals("Recent", past.get(0).getTitle());
        assertEquals("Old", past.get(1).getTitle());
    }

    @Test
    void eventRepository_FindNextEvent_ReturnsClosestFuture() {
        LocalDateTime now = LocalDateTime.now();
        eventRepository.save(Event.builder().title("Far").startTime(now.plusDays(5)).build());
        Event near = eventRepository.save(Event.builder().title("Near").startTime(now.plusHours(1)).build());

        Optional<Event> next = eventRepository.findNextEvent(now);
        assertTrue(next.isPresent());
        assertEquals("Near", next.get().getTitle());
    }

    // ==================== PaymentRepository ====================

    @Test
    void paymentRepository_FindByStatus_ReturnsMatchingPayments() {
        UUID userId = UUID.randomUUID();
        paymentRepository.save(Payment.builder().userId(userId).amount(BigDecimal.TEN).subscriptionMonths(1).currency("RUB").status(Payment.PaymentStatus.PENDING).build());
        paymentRepository.save(Payment.builder().userId(userId).amount(BigDecimal.TEN).subscriptionMonths(1).currency("RUB").status(Payment.PaymentStatus.PAID).build());
        paymentRepository.save(Payment.builder().userId(userId).amount(BigDecimal.TEN).subscriptionMonths(1).currency("RUB").status(Payment.PaymentStatus.PENDING).build());

        List<Payment> pending = paymentRepository.findByStatus(Payment.PaymentStatus.PENDING);
        assertEquals(2, pending.size());
    }

    @Test
    void paymentRepository_FindExpiredPayments_ReturnsOnlyExpiredPending() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        paymentRepository.save(Payment.builder().userId(userId).amount(BigDecimal.TEN).subscriptionMonths(1).currency("RUB")
                .status(Payment.PaymentStatus.PENDING).expiresAt(now.minusHours(1)).build());
        paymentRepository.save(Payment.builder().userId(userId).amount(BigDecimal.TEN).subscriptionMonths(1).currency("RUB")
                .status(Payment.PaymentStatus.PENDING).expiresAt(now.plusHours(1)).build());
        paymentRepository.save(Payment.builder().userId(userId).amount(BigDecimal.TEN).subscriptionMonths(1).currency("RUB")
                .status(Payment.PaymentStatus.PAID).expiresAt(now.minusHours(1)).build());

        List<Payment> expired = paymentRepository.findExpiredPayments(now);
        assertEquals(1, expired.size());
        assertEquals(Payment.PaymentStatus.PENDING, expired.get(0).getStatus());
    }

    @Test
    void paymentRepository_FindPendingPayments_OrdersByCreatedAtDesc() {
        UUID userId = UUID.randomUUID();
        paymentRepository.save(Payment.builder().userId(userId).amount(BigDecimal.TEN).subscriptionMonths(1).currency("RUB")
                .status(Payment.PaymentStatus.PENDING).build());
        paymentRepository.save(Payment.builder().userId(userId).amount(BigDecimal.TEN).subscriptionMonths(1).currency("RUB")
                .status(Payment.PaymentStatus.PENDING).build());

        List<Payment> pending = paymentRepository.findPendingPayments();
        assertEquals(2, pending.size());
    }
}
