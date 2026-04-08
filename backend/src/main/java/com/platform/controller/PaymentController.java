package com.platform.controller;

import com.platform.model.Payment;
import com.platform.model.dto.PaymentDto;
import com.platform.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Создание нового платежа
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PaymentDto> createPayment(
            @RequestBody PaymentDto.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Payment payment = paymentService.createPayment(request.getSubscriptionMonths(), jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(payment));
    }

    /**
     * Получение платежа
     */
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PaymentDto> getPayment(
            @PathVariable String paymentId,
            @AuthenticationPrincipal Jwt jwt) {

        Payment payment = paymentService.getPayment(paymentId, jwt);
        return ResponseEntity.ok(toDto(payment));
    }

    /**
     * Получение всех платежей пользователя
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<PaymentDto>> getMyPayments(
            @AuthenticationPrincipal Jwt jwt) {

        List<Payment> payments = paymentService.getUserPayments(jwt);
        return ResponseEntity.ok(payments.stream().map(this::toDto).collect(Collectors.toList()));
    }

    /**
     * Проверка статуса платежа
     */
    @GetMapping("/{paymentId}/status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PaymentDto> checkPaymentStatus(
            @PathVariable String paymentId,
            @AuthenticationPrincipal Jwt jwt) {

        Payment payment = paymentService.checkPaymentStatus(paymentId, jwt);
        return ResponseEntity.ok(toDto(payment));
    }

    /**
     * Подтверждение платежа админом
     */
    @PostMapping("/{paymentId}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'VYACHESLAV', 'SUPER_ADMIN')")
    public ResponseEntity<PaymentDto> confirmPayment(
            @PathVariable String paymentId,
            @RequestBody PaymentDto.ConfirmRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Payment payment = paymentService.confirmPayment(paymentId, request.getNotes(), jwt);
        return ResponseEntity.ok(toDto(payment));
    }

    /**
     * Получение всех ожидающих платежей (для админа)
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'VYACHESLAV', 'SUPER_ADMIN')")
    public ResponseEntity<List<PaymentDto>> getPendingPayments() {

        List<Payment> payments = paymentService.getPendingPayments();
        return ResponseEntity.ok(payments.stream().map(this::toDto).collect(Collectors.toList()));
    }

    private PaymentDto toDto(Payment payment) {
        return PaymentDto.builder()
            .id(payment.getId())
            .userId(payment.getUserId())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .subscriptionMonths(payment.getSubscriptionMonths())
            .status(payment.getStatus().name())
            .paymentMethod(payment.getPaymentMethod())
            .qrCodeData(payment.getQrCodeData())
            .expiresAt(payment.getExpiresAt())
            .paidAt(payment.getPaidAt())
            .createdAt(payment.getCreatedAt())
            .build();
    }
}
