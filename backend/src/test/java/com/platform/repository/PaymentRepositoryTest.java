package com.platform.repository;

import com.platform.model.entity.Payment;
import com.platform.model.entity.User;
import com.platform.model.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PaymentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PaymentRepository paymentRepository;

    private User user1;
    private User user2;
    private Payment payment1;
    private Payment payment2;
    private Payment payment3;
    private Payment payment4;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        entityManager.clear();

        // Create test users
        user1 = User.builder()
                .id(UUID.randomUUID())
                .username("user1")
                .email("user1@example.com")
                .passwordHash("hash1")
                .build();

        user2 = User.builder()
                .id(UUID.randomUUID())
                .username("user2")
                .email("user2@example.com")
                .passwordHash("hash2")
                .build();

        entityManager.persist(user1);
        entityManager.persist(user2);

        // Create test payments
        payment1 = Payment.builder()
                .id(UUID.randomUUID())
                .user(user1)
                .subscriptionMonths(1)
                .amount(10000)
                .status(PaymentStatus.PENDING)
                .qrCodeData("qr1_base64")
                .createdAt(Instant.now().minusHours(5))
                .build();

        payment2 = Payment.builder()
                .id(UUID.randomUUID())
                .user(user1)
                .subscriptionMonths(3)
                .amount(27000)  // 3 * 10000 * 0.9 = 27000 (10% discount)
                .status(PaymentStatus.CONFIRMED)
                .qrCodeData(null)
                .confirmedAt(Instant.now().minusHours(3))
                .createdAt(Instant.now().minusDays(2))
                .build();

        payment3 = Payment.builder()
                .id(UUID.randomUUID())
                .user(user2)
                .subscriptionMonths(1)
                .amount(10000)
                .status(PaymentStatus.PENDING)
                .qrCodeData("qr3_base64")
                .createdAt(Instant.now().minusHours(2))
                .build();

        payment4 = Payment.builder()
                .id(UUID.randomUUID())
                .user(user1)
                .subscriptionMonths(6)
                .amount(54000)  // 6 * 10000 * 0.9 = 54000 (10% discount)
                .status(PaymentStatus.EXPIRED)
                .qrCodeData(null)
                .createdAt(Instant.now().minusDays(5))
                .expiredAt(Instant.now().minusDays(1))
                .build();

        entityManager.persist(payment1);
        entityManager.persist(payment2);
        entityManager.persist(payment3);
        entityManager.persist(payment4);
        entityManager.flush();
    }

    @Test
    void findByUserId_ValidUser_ReturnsPayments() {
        // Act
        List<Payment> payments = paymentRepository.findByUserId(user1.getId());

        // Assert
        assertThat(payments).hasSize(3); // payment1, payment2, payment4
        assertThat(payments).extracting(Payment::getUser)
                .extracting(User::getId)
                .containsOnly(user1.getId());
    }

    @Test
    void findByUserId_NonExistentUser_ReturnsEmptyList() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        List<Payment> payments = paymentRepository.findByUserId(nonExistentId);

        // Assert
        assertThat(payments).isEmpty();
    }

    @Test
    void findByStatus_ValidStatus_ReturnsPayments() {
        // Act
        List<Payment> pendingPayments = paymentRepository.findByStatus(PaymentStatus.PENDING);

        // Assert
        assertThat(pendingPayments).hasSize(2); // payment1, payment3
        assertThat(pendingPayments).extracting(Payment::getStatus)
                .containsOnly(PaymentStatus.PENDING);
    }

    @Test
    void findByStatus_ConfirmedStatus_ReturnsPayments() {
        // Act
        List<Payment> confirmedPayments = paymentRepository.findByStatus(PaymentStatus.CONFIRMED);

        // Assert
        assertThat(confirmedPayments).hasSize(1); // payment2
        assertThat(confirmedPayments.get(0).getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(confirmedPayments.get(0).getConfirmedAt()).isNotNull();
    }

    @Test
    void findByUserIdAndStatus_ValidParams_ReturnsPayments() {
        // Act
        List<Payment> payments = paymentRepository.findByUserIdAndStatus(
                user1.getId(),
                PaymentStatus.PENDING
        );

        // Assert
        assertThat(payments).hasSize(1); // payment1
        assertThat(payments.get(0).getUser().getId()).isEqualTo(user1.getId());
        assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void findByUserIdAndStatus_NoMatches_ReturnsEmptyList() {
        // Act
        List<Payment> payments = paymentRepository.findByUserIdAndStatus(
                user2.getId(),
                PaymentStatus.CONFIRMED
        );

        // Assert
        assertThat(payments).isEmpty();
    }

    @Test
    void findByCreatedAtBefore_ValidDate_ReturnsOlderPayments() {
        // Arrange
        Instant cutoff = Instant.now().minusDays(3);

        // Act
        List<Payment> payments = paymentRepository.findByCreatedAtBefore(cutoff);

        // Assert
        assertThat(payments).hasSize(2); // payment2, payment4
        assertThat(payments).extracting(Payment::getId)
                .containsExactlyInAnyOrder(payment2.getId(), payment4.getId());
    }

    @Test
    void findByCreatedAtAfter_ValidDate_ReturnsNewerPayments() {
        // Arrange
        Instant cutoff = Instant.now().minusDays(3);

        // Act
        List<Payment> payments = paymentRepository.findByCreatedAtAfter(cutoff);

        // Assert
        assertThat(payments).hasSize(2); // payment1, payment3
        assertThat(payments).extracting(Payment::getId)
                .containsExactlyInAnyOrder(payment1.getId(), payment3.getId());
    }

    @Test
    void findByCreatedAtBetween_ValidRange_ReturnsPayments() {
        // Arrange
        Instant start = Instant.now().minusDays(4);
        Instant end = Instant.now().minusDays(1);

        // Act
        List<Payment> payments = paymentRepository.findByCreatedAtBetween(start, end);

        // Assert
        assertThat(payments).hasSize(1); // payment2
        assertThat(payments.get(0).getId()).isEqualTo(payment2.getId());
    }

    @Test
    void findAllByOrderByCreatedAtDesc_ReturnsSortedPayments() {
        // Act
        List<Payment> payments = paymentRepository.findAllByOrderByCreatedAtDesc();

        // Assert
        assertThat(payments).hasSize(4);
        // Should be sorted by createdAt descending (newest first)
        assertThat(payments.get(0).getId()).isEqualTo(payment3.getId()); // 2 hours ago
        assertThat(payments.get(1).getId()).isEqualTo(payment1.getId()); // 5 hours ago
        assertThat(payments.get(2).getId()).isEqualTo(payment2.getId()); // 2 days ago
        assertThat(payments.get(3).getId()).isEqualTo(payment4.getId()); // 5 days ago
    }

    @Test
    void countByUserId_ValidUser_ReturnsCount() {
        // Act
        long count = paymentRepository.countByUserId(user1.getId());

        // Assert
        assertThat(count).isEqualTo(3); // payment1, payment2, payment4
    }

    @Test
    void countByUserId_NonExistentUser_ReturnsZero() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        long count = paymentRepository.countByUserId(nonExistentId);

        // Assert
        assertThat(count).isEqualTo(0);
    }

    @Test
    void countByStatus_ValidStatus_ReturnsCount() {
        // Act
        long pendingCount = paymentRepository.countByStatus(PaymentStatus.PENDING);
        long confirmedCount = paymentRepository.countByStatus(PaymentStatus.CONFIRMED);
        long expiredCount = paymentRepository.countByStatus(PaymentStatus.EXPIRED);

        // Assert
        assertThat(pendingCount).isEqualTo(2); // payment1, payment3
        assertThat(confirmedCount).isEqualTo(1); // payment2
        assertThat(expiredCount).isEqualTo(1); // payment4
    }

    @Test
    void updateStatus_ValidPayment_UpdatesStatus() {
        // Arrange
        Instant confirmedAt = Instant.now();

        // Act
        int updatedCount = paymentRepository.updateStatus(
                payment1.getId(),
                PaymentStatus.CONFIRMED,
                confirmedAt
        );
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(updatedCount).isEqualTo(1);
        Payment updatedPayment = entityManager.find(Payment.class, payment1.getId());
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(updatedPayment.getConfirmedAt()).isEqualTo(confirmedAt);
        assertThat(updatedPayment.getQrCodeData()).isNull(); // Should be cleared
    }

    @Test
    void updateStatus_NonExistentPayment_ReturnsZero() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        int updatedCount = paymentRepository.updateStatus(
                nonExistentId,
                PaymentStatus.CONFIRMED,
                Instant.now()
        );

        // Assert
        assertThat(updatedCount).isEqualTo(0);
    }

    @Test
    void updateStatusToExpired_ValidPayment_UpdatesStatus() {
        // Arrange
        Instant expiredAt = Instant.now();

        // Act
        int updatedCount = paymentRepository.updateStatusToExpired(
                payment1.getId(),
                expiredAt
        );
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(updatedCount).isEqualTo(1);
        Payment updatedPayment = entityManager.find(Payment.class, payment1.getId());
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(updatedPayment.getExpiredAt()).isEqualTo(expiredAt);
        assertThat(updatedPayment.getQrCodeData()).isNull(); // Should be cleared
    }

    @Test
    void findPendingPaymentsOlderThan_ValidDate_ReturnsPayments() {
        // Arrange
        Instant cutoff = Instant.now().minusHours(4);

        // Act
        List<Payment> payments = paymentRepository.findPendingPaymentsOlderThan(cutoff);

        // Assert
        assertThat(payments).hasSize(1); // payment1 (created 5 hours ago)
        assertThat(payments.get(0).getId()).isEqualTo(payment1.getId());
        assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void findPendingPaymentsOlderThan_NoMatches_ReturnsEmptyList() {
        // Arrange
        Instant cutoff = Instant.now().minusHours(1);

        // Act
        List<Payment> payments = paymentRepository.findPendingPaymentsOlderThan(cutoff);

        // Assert
        assertThat(payments).isEmpty(); // payment1 is 5 hours old, payment3 is 2 hours old
    }

    @Test
    void findTopByUserIdOrderByCreatedAtDesc_ValidUser_ReturnsLatestPayment() {
        // Act
        Optional<Payment> latestPayment = paymentRepository.findTopByUserIdOrderByCreatedAtDesc(user1.getId());

        // Assert
        assertThat(latestPayment).isPresent();
        assertThat(latestPayment.get().getId()).isEqualTo(payment1.getId()); // Created 5 hours ago (newest for user1)
    }

    @Test
    void findTopByUserIdOrderByCreatedAtDesc_NonExistentUser_ReturnsEmpty() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        Optional<Payment> latestPayment = paymentRepository.findTopByUserIdOrderByCreatedAtDesc(nonExistentId);

        // Assert
        assertThat(latestPayment).isEmpty();
    }

    @Test
    void findByAmountGreaterThanEqual_ValidAmount_ReturnsPayments() {
        // Act
        List<Payment> payments = paymentRepository.findByAmountGreaterThanEqual(20000);

        // Assert
        assertThat(payments).hasSize(2); // payment2 (27000), payment4 (54000)
        assertThat(payments).extracting(Payment::getAmount)
                .containsExactlyInAnyOrder(27000L, 54000L);
    }

    @Test
    void findByAmountLessThanEqual_ValidAmount_ReturnsPayments() {
        // Act
        List<Payment> payments = paymentRepository.findByAmountLessThanEqual(15000);

        // Assert
        assertThat(payments).hasSize(2); // payment1 (10000), payment3 (10000)
        assertThat(payments).extracting(Payment::getAmount)
                .containsOnly(10000L);
    }

    @Test
    void findBySubscriptionMonths_ValidMonths_ReturnsPayments() {
        // Act
        List<Payment> payments = paymentRepository.findBySubscriptionMonths(1);

        // Assert
        assertThat(payments).hasSize(2); // payment1, payment3
        assertThat(payments).extracting(Payment::getSubscriptionMonths)
                .containsOnly(1);
    }

    @Test
    void findBySubscriptionMonths_NoMatches_ReturnsEmptyList() {
        // Act
        List<Payment> payments = paymentRepository.findBySubscriptionMonths(12);

        // Assert
        assertThat(payments).isEmpty();
    }

    @Test
    void findAllByUserOrderByCreatedAtDesc_ValidUser_ReturnsSortedPayments() {
        // Act
        Page<Payment> page = paymentRepository.findAllByUserOrderByCreatedAtDesc(
                user1,
                PageRequest.of(0, 10)
        );

        // Assert
        assertThat(page.getTotalElements()).isEqualTo(3); // payment1, payment2, payment4
        assertThat(page.getContent()).extracting(Payment::getId)
                .containsExactly(payment1.getId(), payment2.getId(), payment4.getId()); // Sorted descending
    }

    @Test
    void findAllByUserOrderByCreatedAtDesc_WithPagination_ReturnsPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Act
        Page<Payment> page = paymentRepository.findAllByUserOrderByCreatedAtDesc(user1, pageable);

        // Assert
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent().get(0).getId()).isEqualTo(payment1.getId()); // Newest
        assertThat(page.getContent().get(1).getId()).isEqualTo(payment2.getId());
    }

    @Test
    void deleteByUserId_ValidUser_DeletesPayments() {
        // Act
        long deletedCount = paymentRepository.deleteByUserId(user1.getId());
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(deletedCount).isEqualTo(3); // payment1, payment2, payment4
        List<Payment> remainingPayments = paymentRepository.findAll();
        assertThat(remainingPayments).hasSize(1); // payment3
        assertThat(remainingPayments.get(0).getUser().getId()).isEqualTo(user2.getId());
    }

    @Test
    void deleteByUserId_NonExistentUser_ReturnsZero() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        long deletedCount = paymentRepository.deleteByUserId(nonExistentId);

        // Assert
        assertThat(deletedCount).isEqualTo(0);
    }

    @Test
    void sumAmountByUserId_ValidUser_ReturnsTotalAmount() {
        // Act
        Long totalAmount = paymentRepository.sumAmountByUserId(user1.getId());

        // Assert
        assertThat(totalAmount).isEqualTo(91000L); // 10000 + 27000 + 54000
    }

    @Test
    void sumAmountByUserId_NoPayments_ReturnsNull() {
        // Arrange
        User newUser = User.builder()
                .id(UUID.randomUUID())
                .username("newuser")
                .email("new@example.com")
                .passwordHash("hash")
                .build();
        entityManager.persist(newUser);
        entityManager.flush();

        // Act
        Long totalAmount = paymentRepository.sumAmountByUserId(newUser.getId());

        // Assert
        assertThat(totalAmount).isNull();
    }

    @Test
    void sumAmountByStatus_ValidStatus_ReturnsTotalAmount() {
        // Act
        Long pendingTotal = paymentRepository.sumAmountByStatus(PaymentStatus.PENDING);
        Long confirmedTotal = paymentRepository.sumAmountByStatus(PaymentStatus.CONFIRMED);

        // Assert
        assertThat(pendingTotal).isEqualTo(20000L); // 10000 + 10000
        assertThat(confirmedTotal).isEqualTo(27000L); // 27000
    }

    @Test
    void findConfirmedPaymentsAfter_ValidDate_ReturnsPayments() {
        // Arrange
        Instant afterDate = Instant.now().minusDays(3);

        // Act
        List<Payment> payments = paymentRepository.findConfirmedPaymentsAfter(afterDate);

        // Assert
        assertThat(payments).hasSize(1); // payment2
        assertThat(payments.get(0).getId()).isEqualTo(payment2.getId());
        assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    }

    @Test
    void findConfirmedPaymentsAfter_NoMatches_ReturnsEmptyList() {
        // Arrange
        Instant afterDate = Instant.now().minusHours(1);

        // Act
        List<Payment> payments = paymentRepository.findConfirmedPaymentsAfter(afterDate);

        // Assert
        assertThat(payments).isEmpty();
    }
}