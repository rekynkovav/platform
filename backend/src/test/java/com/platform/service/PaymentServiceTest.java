package com.platform.service;

import com.platform.exception.BusinessException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Payment;
import com.platform.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private QRCodeService qrCodeService;

    @InjectMocks
    private PaymentService paymentService;

    private Jwt jwt;
    private String userId;
    private UUID userIdUuid;

    @BeforeEach
    void setUp() {
        userId = "550e8400-e29b-41d4-a716-446655440000";
        userIdUuid = UUID.fromString(userId);
        jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("preferred_username", "testuser")
                .build();
    }

    @Test
    void createPayment_NoExistingPending_CreatesPayment() {
        when(paymentRepository.findByUserIdAndStatus(eq(userIdUuid), eq(Payment.PaymentStatus.PENDING)))
                .thenReturn(List.of());
        when(qrCodeService.generatePaymentQRData(any(), anyString(), anyString())).thenReturn("qr-data");
        when(qrCodeService.generateQRCodeBase64(anyString())).thenReturn("base64-qr");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.createPayment(1, jwt);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("10000"));
        assertThat(result.getSubscriptionMonths()).isEqualTo(1);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPayment_ExistingPending_ThrowsException() {
        Payment existing = Payment.builder()
                .id(UUID.randomUUID())
                .userId(userIdUuid)
                .amount(new BigDecimal("10000"))
                .status(Payment.PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByUserIdAndStatus(eq(userIdUuid), eq(Payment.PaymentStatus.PENDING)))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> paymentService.createPayment(1, jwt))
                .isInstanceOf(BusinessException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void getPayment_ExistingPayment_ReturnsPayment() {
        String paymentId = UUID.randomUUID().toString();
        UUID paymentIdUuid = UUID.fromString(paymentId);
        Payment payment = Payment.builder()
                .id(paymentIdUuid)
                .userId(userIdUuid)
                .amount(new BigDecimal("10000"))
                .status(Payment.PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByIdAndUserId(eq(paymentIdUuid), eq(userIdUuid)))
                .thenReturn(Optional.of(payment));

        Payment result = paymentService.getPayment(paymentId, jwt);

        assertThat(result).isNotNull();
        assertThat(result.getId().toString()).isEqualTo(paymentId);
    }

    @Test
    void getPayment_NonExistent_ThrowsException() {
        String paymentId = UUID.randomUUID().toString();
        UUID paymentIdUuid = UUID.fromString(paymentId);
        when(paymentRepository.findByIdAndUserId(eq(paymentIdUuid), eq(userIdUuid)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPayment(paymentId, jwt))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserPayments_ReturnsPayments() {
        Payment p1 = Payment.builder()
                .id(UUID.randomUUID())
                .userId(userIdUuid)
                .amount(new BigDecimal("10000"))
                .status(Payment.PaymentStatus.PAID)
                .build();

        when(paymentRepository.findByUserId(userIdUuid)).thenReturn(List.of(p1));

        List<Payment> payments = paymentService.getUserPayments(jwt);

        assertThat(payments).hasSize(1);
    }

    @Test
    void confirmPayment_PendingPayment_ConfirmsAndUpdatesRole() {
        String paymentId = UUID.randomUUID().toString();
        Payment payment = Payment.builder()
                .id(UUID.fromString(paymentId))
                .userId(userIdUuid)
                .amount(new BigDecimal("10000"))
                .status(Payment.PaymentStatus.PENDING)
                .subscriptionMonths(1)
                .build();

        when(paymentRepository.findById(UUID.fromString(paymentId))).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.confirmPayment(paymentId, "Confirmed manually", jwt);

        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.CONFIRMED);
        assertThat(result.getPaidAt()).isNotNull();
        assertThat(result.getConfirmedBy()).isEqualTo(userId);
        verify(keycloakService).updateUserRole(eq(userId), eq("USER_0"), eq("USER_1"));
    }

    @Test
    void confirmPayment_AlreadyProcessed_ThrowsException() {
        String paymentId = UUID.randomUUID().toString();
        Payment payment = Payment.builder()
                .id(UUID.fromString(paymentId))
                .userId(userIdUuid)
                .amount(new BigDecimal("10000"))
                .status(Payment.PaymentStatus.CONFIRMED)
                .build();

        when(paymentRepository.findById(UUID.fromString(paymentId))).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirmPayment(paymentId, "notes", jwt))
                .isInstanceOf(IllegalStateException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void expireOldPayments_ExpiresPendingPayments() {
        Payment expired = Payment.builder()
                .id(UUID.randomUUID())
                .userId(userIdUuid)
                .amount(new BigDecimal("10000"))
                .status(Payment.PaymentStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(paymentRepository.findExpiredPayments(any(LocalDateTime.class))).thenReturn(List.of(expired));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.expireOldPayments();

        assertThat(expired.getStatus()).isEqualTo(Payment.PaymentStatus.EXPIRED);
        verify(paymentRepository).save(expired);
    }

    @Test
    void getPendingPayments_ReturnsList() {
        when(paymentRepository.findPendingPayments()).thenReturn(List.of());

        List<Payment> pending = paymentService.getPendingPayments();

        assertThat(pending).isEmpty();
    }
}
