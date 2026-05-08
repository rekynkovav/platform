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

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_PREMIUM', 'ROLE_ADVANCED', 'ROLE_CREATOR', 'ROLE_ADMIN', 'ROLE_MODERATOR', 'ROLE_SPORT_ADMIN', 'ROLE_NUTRITION_ADMIN', 'ROLE_CONSULTANT_ADMIN', 'ROLE_OWNER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<PaymentDto> createPayment(
            @RequestBody PaymentDto.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Payment payment = paymentService.createPayment(request.getSubscriptionMonths(), jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(payment));
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_PREMIUM', 'ROLE_ADVANCED', 'ROLE_CREATOR', 'ROLE_ADMIN', 'ROLE_MODERATOR', 'ROLE_SPORT_ADMIN', 'ROLE_NUTRITION_ADMIN', 'ROLE_CONSULTANT_ADMIN', 'ROLE_OWNER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<PaymentDto> getPayment(
            @PathVariable String paymentId,
            @AuthenticationPrincipal Jwt jwt) {

        Payment payment = paymentService.getPayment(paymentId, jwt);
        return ResponseEntity.ok(toDto(payment));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_PREMIUM', 'ROLE_ADVANCED', 'ROLE_CREATOR', 'ROLE_ADMIN', 'ROLE_MODERATOR', 'ROLE_SPORT_ADMIN', 'ROLE_NUTRITION_ADMIN', 'ROLE_CONSULTANT_ADMIN', 'ROLE_OWNER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<PaymentDto>> getMyPayments(
            @AuthenticationPrincipal Jwt jwt) {

        List<Payment> payments = paymentService.getUserPayments(jwt);
        return ResponseEntity.ok(payments.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/{paymentId}/status")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_PREMIUM', 'ROLE_ADVANCED', 'ROLE_CREATOR', 'ROLE_ADMIN', 'ROLE_MODERATOR', 'ROLE_SPORT_ADMIN', 'ROLE_NUTRITION_ADMIN', 'ROLE_CONSULTANT_ADMIN', 'ROLE_OWNER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<PaymentDto> checkPaymentStatus(
            @PathVariable String paymentId,
            @AuthenticationPrincipal Jwt jwt) {

        Payment payment = paymentService.checkPaymentStatus(paymentId, jwt);
        return ResponseEntity.ok(toDto(payment));
    }

    @PostMapping("/{paymentId}/confirm")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<PaymentDto> confirmPayment(
            @PathVariable String paymentId,
            @RequestBody PaymentDto.ConfirmRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Payment payment = paymentService.confirmPayment(paymentId, request.getNotes(), jwt);
        return ResponseEntity.ok(toDto(payment));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER', 'ROLE_SUPER_ADMIN')")
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
