package com.platform.service;

import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Payment;
import com.platform.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KeycloakService keycloakService;

    @Transactional
    public Payment createPayment(Integer subscriptionMonths, Jwt jwt) {
        String userId = jwt.getSubject();
        String username = jwt.getClaim("preferred_username");

        BigDecimal amount = calculateAmount(subscriptionMonths);
        String paymentId = generatePaymentId();
        String qrCodeData = generateSBPQRData(amount, paymentId, username);

        Payment payment = Payment.builder()
            .id(paymentId)
            .userId(userId)
            .amount(amount)
            .currency("RUB")
            .subscriptionMonths(subscriptionMonths)
            .status(Payment.PaymentStatus.PENDING)
            .paymentMethod("SBP")
            .qrCodeData(qrCodeData)
            .expiresAt(LocalDateTime.now().plusHours(24))
            .build();

        payment = paymentRepository.save(payment);

        log.info("Payment created: {} for user: {}, amount: {}", paymentId, userId, amount);
        return payment;
    }

    public Payment getPayment(String paymentId, Jwt jwt) {
        String userId = jwt.getSubject();
        return paymentRepository.findByIdAndUserId(paymentId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
    }

    public List<Payment> getUserPayments(Jwt jwt) {
        String userId = jwt.getSubject();
        return paymentRepository.findByUserId(userId);
    }

    public Payment checkPaymentStatus(String paymentId, Jwt jwt) {
        Payment payment = getPayment(paymentId, jwt);
        log.info("Checking payment status: {}, current status: {}", paymentId, payment.getStatus());
        return payment;
    }

    @Transactional
    public Payment confirmPayment(String paymentId, String notes, Jwt jwt) {
        String adminId = jwt.getSubject();

        Payment payment = paymentRepository.findById(paymentId)
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
            keycloakService.updateUserRole(payment.getUserId(), "USER", "PREMIUM");
            log.info("User role updated: {} -> PREMIUM", payment.getUserId());
        } catch (Exception e) {
            log.error("Failed to update user role in Keycloak", e);
            throw new RuntimeException("Failed to update user role: " + e.getMessage());
        }

        log.info("Payment confirmed: {} by admin: {}", paymentId, adminId);
        return payment;
    }

    public List<Payment> getPendingPayments() {
        return paymentRepository.findPendingPayments();
    }

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

    private String generatePaymentId() {
        long timestamp = System.currentTimeMillis();
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("PAY-%d-%s", timestamp, random);
    }

    private String generateSBPQRData(BigDecimal amount, String paymentId, String username) {
        String recipientName = "БФАДД";
        String account = "40703810512000002359";
        String bank = "ВОЛОГОДСКОЕ ОТДЕЛЕНИЕ N8638 ПАО СБЕРБАНК г. Вологда";
        String bik = "041909644";
        String corrAccount = "30101810900000000644";
        String inn = "9715470089";
        String kpp = "771501001";
        String purpose = String.format("Подписка %s", paymentId);

        return String.format(
            "ST00012|Name=%s|PersonalAcc=%s|BankName=%s|BIC=%s|CorrespAcc=%s|Sum=%d|Purpose=%s|PayeeINN=%s|KPP=%s",
            recipientName, account, bank, bik, corrAccount,
            amount.multiply(new BigDecimal("100")).intValue(),
            purpose, inn, kpp
        );
    }
}
