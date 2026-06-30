package com.platform.repository;

import com.platform.config.TestJpaConfig;
import com.platform.model.Payment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(TestJpaConfig.class)
@ActiveProfiles("test")
class EventPaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

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

        List<Payment> pending = paymentRepository.findPendingPayments(Pageable.unpaged());
        assertEquals(2, pending.size());
    }
}
