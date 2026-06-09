package com.platform.service;

import com.platform.exception.ResourceNotFoundException;
import com.platform.exception.BusinessException;
import com.platform.exception.KeycloakException;
import com.platform.exception.PaymentProcessingException;
import com.platform.model.Payment;
import com.platform.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.dao.OptimisticLockingFailureException;
import io.micrometer.core.annotation.Timed;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    // ВАЖНО: Для предотвращения deadlock-ов всегда соблюдать порядок блокировок:
    // 1. Сначала блокировать пользователей (users)
    // 2. Потом блокировать платежи (payments)
    // Это должно быть согласовано со всеми другими сервисами

    private final PaymentRepository paymentRepository;
    private final KeycloakService keycloakService;
    private final QRCodeService qrCodeService;

    @Timed(value = "payment.create", description = "Time to create payment")
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 1000)
    )
    @Transactional(
        isolation = Isolation.READ_COMMITTED,
        rollbackFor = Exception.class,
        timeout = 30
    )
    public Payment createPayment(Integer subscriptionMonths, Jwt jwt) {
        String userId = jwt.getSubject();
        String username = jwt.getClaim("preferred_username");

        // Проверка на существующий активный платёж
        List<Payment> pendingPayments = paymentRepository.findByUserIdAndStatus(UUID.fromString(userId), Payment.PaymentStatus.PENDING);
        if (!pendingPayments.isEmpty()) {
            throw new BusinessException("У вас уже есть активный платёж. Дождитесь подтверждения.");
        }

        BigDecimal amount = calculateAmount(subscriptionMonths);
        UUID paymentUuid = UUID.randomUUID();
        String paymentId = generatePaymentId(paymentUuid);
        String qrData = qrCodeService.generatePaymentQRData(amount, paymentId, username);
        String qrCodeBase64 = qrCodeService.generateQRCodeBase64(qrData);

        Payment payment = Payment.builder()
            .id(paymentUuid)
            .userId(UUID.fromString(userId))
            .amount(amount)
            .currency("RUB")
            .subscriptionMonths(subscriptionMonths)
            .status(Payment.PaymentStatus.PENDING)
            .paymentMethod("SBP")
            .qrCodeData(qrData)
            .expiresAt(LocalDateTime.now().plusHours(24))
            .build();

        payment = paymentRepository.save(payment);

        log.info("Payment created: {} for user: {}, amount: {}", paymentId, userId, amount);
        return payment;
    }

    @Timed(value = "payment.get", description = "Time to get payment")
    @Transactional(readOnly = true, timeout = 10)
    public Payment getPayment(String paymentId, Jwt jwt) {
        String userId = jwt.getSubject();
        return paymentRepository.findByIdAndUserId(UUID.fromString(paymentId), UUID.fromString(userId))
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
    }

    @Timed(value = "payment.get_user_payments", description = "Time to get user payments")
    public List<Payment> getUserPayments(Jwt jwt) {
        String userId = jwt.getSubject();
        return paymentRepository.findByUserId(UUID.fromString(userId));
    }

    @Timed(value = "payment.check_status", description = "Time to check payment status")
    public Payment checkPaymentStatus(String paymentId, Jwt jwt) {
        Payment payment = getPayment(paymentId, jwt);
        log.info("Checking payment status: {}, current status: {}", paymentId, payment.getStatus());
        return payment;
    }

    @Timed(value = "payment.confirm", description = "Time to confirm payment")
    @Transactional(
        isolation = Isolation.READ_COMMITTED,
        rollbackFor = Exception.class,
        timeout = 10
    )
    public Payment confirmPayment(String paymentId, String notes, Jwt jwt) {
        String adminId = jwt.getSubject();

        Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment already processed");
        }

        payment.setStatus(Payment.PaymentStatus.CONFIRMED);
        payment.setPaidAt(LocalDateTime.now());
        payment.setConfirmedBy(adminId);
        payment.setNotes(notes);

        payment = paymentRepository.save(payment);

        try {
            keycloakService.updateUserRole(payment.getUserId().toString(), "USER_0", "USER_1");
            log.info("User role updated: {} -> USER_1", payment.getUserId());
        } catch (Exception e) {
            log.error("Failed to update user role in Keycloak", e);
        }

        log.info("Payment confirmed: {} by admin: {}", paymentId, adminId);
        return payment;
    }

    @Timed(value = "payment.get_pending", description = "Time to get pending payments")
    public List<Payment> getPendingPayments() {
        return paymentRepository.findPendingPayments();
    }

    @Timed(value = "payment.expire_old", description = "Time to expire old payments")
    @Transactional
    public void expireOldPayments() {
        List<Payment> expiredPayments = paymentRepository.findExpiredPayments(LocalDateTime.now());

        for (Payment payment : expiredPayments) {
            payment.setStatus(Payment.PaymentStatus.EXPIRED);
            paymentRepository.save(payment);
            log.info("Payment expired: {}", payment.getId());
        }
    }

    private BigDecimal calculateAmount(Integer months) {
        BigDecimal basePrice = new BigDecimal("10000");

        return switch (months) {
            case 1 -> basePrice;
            case 3 -> basePrice.multiply(new BigDecimal("2.7"));
            case 6 -> basePrice.multiply(new BigDecimal("4.8"));
            case 12 -> basePrice.multiply(new BigDecimal("8.4"));
            default -> throw new IllegalArgumentException("Invalid subscription period: " + months);
        };
    }

    private String generatePaymentId(UUID uuid) {
        long timestamp = System.currentTimeMillis();
        String random = uuid.toString().substring(0, 8).toUpperCase();
        return String.format("PAY-%d-%s", timestamp, random);
    }

    @Timed(value = "payment.find_by_user_and_status", description = "Time to find payments by user and status")
    public List<Payment> findByUserIdAndStatus(String userId, Payment.PaymentStatus status) {
        return paymentRepository.findByUserIdAndStatus(UUID.fromString(userId), status);
    }

}
