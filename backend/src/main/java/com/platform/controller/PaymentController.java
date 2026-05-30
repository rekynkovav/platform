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
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('USER_0')")
    public ResponseEntity<PaymentDto> createPayment(
            @Valid @RequestBody PaymentDto.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Payment payment = paymentService.createPayment(request.getSubscriptionMonths(), jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(payment));
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasRole('USER_0')")
    public ResponseEntity<PaymentDto> getPayment(
            @PathVariable String paymentId,
            @AuthenticationPrincipal Jwt jwt) {

        Payment payment = paymentService.getPayment(paymentId, jwt);
        return ResponseEntity.ok(toDto(payment));
    }

    @GetMapping("/{paymentId}/qr")
    @PreAuthorize("hasRole('USER_0')")
    public ResponseEntity<PaymentDto> getPaymentQR(
            @PathVariable String paymentId,
            @AuthenticationPrincipal Jwt jwt) {

        Payment payment = paymentService.getPayment(paymentId, jwt);
        return ResponseEntity.ok(toDto(payment));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER_0')")
    public ResponseEntity<List<PaymentDto>> getMyPayments(
            @AuthenticationPrincipal Jwt jwt) {

        List<Payment> payments = paymentService.getUserPayments(jwt);
        return ResponseEntity.ok(payments.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/{paymentId}/status")
    @PreAuthorize("hasRole('USER_0')")
    public ResponseEntity<PaymentDto> checkPaymentStatus(
            @PathVariable String paymentId,
            @AuthenticationPrincipal Jwt jwt) {

        Payment payment = paymentService.checkPaymentStatus(paymentId, jwt);
        return ResponseEntity.ok(toDto(payment));
    }

    @PostMapping("/{paymentId}/confirm")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'VYACHESLAV', 'SUPER_ADMIN')")
    public ResponseEntity<PaymentDto> confirmPayment(
            @PathVariable String paymentId,
            @Valid @RequestBody PaymentDto.ConfirmRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Payment payment = paymentService.confirmPayment(paymentId, request.getNotes(), jwt);
        return ResponseEntity.ok(toDto(payment));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'VYACHESLAV', 'SUPER_ADMIN')")
    public ResponseEntity<List<PaymentDto>> getPendingPayments() {

        List<Payment> payments = paymentService.getPendingPayments();
        return ResponseEntity.ok(payments.stream().map(this::toDto).collect(Collectors.toList()));
    }

    private PaymentDto toDto(Payment payment) {
        return PaymentDto.builder()
            .id(payment.getId() != null ? payment.getId().toString() : null)
            .userId(payment.getUserId() != null ? payment.getUserId().toString() : null)
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
