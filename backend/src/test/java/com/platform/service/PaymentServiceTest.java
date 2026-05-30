package com.platform.service;

import com.platform.exception.BusinessException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Payment;
import com.platform.model.dto.PaymentDto;
import com.platform.model.enums.PaymentStatus;
import com.platform.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    private String userId;
    private UUID userIdUuid;
    private String paymentId;
    private UUID paymentIdUuid;

    @BeforeEach
    void setUp() {
        userId = "user-123";
        userIdUuid = UUID.fromString(userId);
        paymentId = "payment-456";
        paymentIdUuid = UUID.fromString(paymentId);
    }

    @Test
    void createPayment_1Month_CreatesWithCorrectAmount() {
        // Arrange
        int months = 1;
        int expectedAmount = 10000;  // 1 month = 10000

        when(paymentRepository.findByUserIdAndStatus(userIdUuid, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment payment = inv.getArgument(0);
            payment.setId(paymentIdUuid);
            return payment;
        });
        when(qrCodeService.generateQRCode(anyString())).thenReturn("qr-code-data");

        // Act
        PaymentDto result = paymentService.createPayment(userId, months);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userIdUuid);
        assertThat(result.getSubscriptionMonths()).isEqualTo(months);
        assertThat(result.getAmount()).isEqualTo(expectedAmount);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPayment_3Months_CreatesWithDiscount() {
        // Arrange
        int months = 3;
        int expectedAmount = 27000;  // 3 * 10000 * 0.9 = 27000 (10% discount)

        when(paymentRepository.findByUserIdAndStatus(userIdUuid, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment payment = inv.getArgument(0);
            payment.setId(paymentIdUuid);
            return payment;
        });
        when(qrCodeService.generateQRCode(anyString())).thenReturn("qr-code-data");

        // Act
        PaymentDto result = paymentService.createPayment(userId, months);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(expectedAmount);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPayment_6Months_CreatesWith20PercentDiscount() {
        // Arrange
        int months = 6;
        int expectedAmount = 48000;  // 6 * 10000 * 0.8 = 48000 (20% discount)

        when(paymentRepository.findByUserIdAndStatus(userIdUuid, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment payment = inv.getArgument(0);
            payment.setId(paymentIdUuid);
            return payment;
        });
        when(qrCodeService.generateQRCode(anyString())).thenReturn("qr-code-data");

        // Act
        PaymentDto result = paymentService.createPayment(userId, months);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(expectedAmount);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPayment_12Months_CreatesWith30PercentDiscount() {
        // Arrange
        int months = 12;
        int expectedAmount = 84000;  // 12 * 10000 * 0.7 = 84000 (30% discount)

        when(paymentRepository.findByUserIdAndStatus(userIdUuid, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment payment = inv.getArgument(0);
            payment.setId(paymentIdUuid);
            return payment;
        });
        when(qrCodeService.generateQRCode(anyString())).thenReturn("qr-code-data");

        // Act
        PaymentDto result = paymentService.createPayment(userId, months);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(expectedAmount);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPayment_InvalidMonths_ThrowsException() {
        // Arrange
        int invalidMonths = 0;

        // Act & Assert
        assertThatThrownBy(() -> paymentService.createPayment(userId, invalidMonths))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid subscription months");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createPayment_ExistingPendingPayment_ThrowsException() {
        // Arrange
        int months = 1;
        Payment existingPayment = Payment.builder()
                .id(paymentIdUuid)
                .userId(userIdUuid)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByUserIdAndStatus(userIdUuid, PaymentStatus.PENDING))
                .thenReturn(Optional.of(existingPayment));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.createPayment(userId, months))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pending payment");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void confirmPayment_PendingToConfirmed_UpdatesStatusAndRole() {
        // Arrange
        Payment payment = Payment.builder()
                .id(paymentIdUuid)
                .userId(userIdUuid)
                .subscriptionMonths(1)
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now().minusMinutes(30))
                .build();

        when(paymentRepository.findById(paymentIdUuid)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Act
        paymentService.confirmPayment(paymentId);

        // Assert
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(payment.getConfirmedAt()).isNotNull();
        verify(keycloakService).updateUserRole(userId, 1);
        verify(paymentRepository).save(payment);
    }

    @Test
    void confirmPayment_AlreadyProcessed_ThrowsException() {
        // Arrange
        Payment payment = Payment.builder()
                .id(paymentIdUuid)
                .userId(userIdUuid)
                .status(PaymentStatus.CONFIRMED)  // Already confirmed
                .confirmedAt(Instant.now().minusMinutes(10))
                .build();

        when(paymentRepository.findById(paymentIdUuid)).thenReturn(Optional.of(payment));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.confirmPayment(paymentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already processed");

        verify(keycloakService, never()).updateUserRole(any(), anyInt());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void expireOldPayments_ExpiresPaymentsOlderThan24Hours() {
        // Arrange
        Payment oldPayment = Payment.builder()
                .id(paymentIdUuid)
                .userId(userIdUuid)
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now().minus(25, ChronoUnit.HOURS))  // 25 hours old
                .build();

        Payment recentPayment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(userIdUuid)
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now().minus(23, ChronoUnit.HOURS))  // 23 hours old
                .build();

        when(paymentRepository.findByStatus(PaymentStatus.PENDING))
                .thenReturn(List.of(oldPayment, recentPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        paymentService.expireOldPayments();

        // Assert
        assertThat(oldPayment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(recentPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);  // Should not expire
        verify(paymentRepository, times(1)).save(oldPayment);
        verify(paymentRepository, never()).save(recentPayment);
    }

    @Test
    void findByUserIdAndStatus_DelegatesToRepository() {
        // Arrange
        PaymentStatus status = PaymentStatus.CONFIRMED;
        Payment payment = Payment.builder()
                .id(paymentIdUuid)
                .userId(userIdUuid)
                .status(status)
                .build();

        when(paymentRepository.findByUserIdAndStatus(userIdUuid, status))
                .thenReturn(List.of(payment));

        // Act
        List<PaymentDto> result = paymentService.findByUserIdAndStatus(userId, status);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(paymentIdUuid);
        assertThat(result.get(0).getStatus()).isEqualTo(status);
        verify(paymentRepository).findByUserIdAndStatus(userIdUuid, status);
    }

    @Test
    void getPayment_CorrectUser_ReturnsPayment() {
        // Arrange
        Payment payment = Payment.builder()
                .id(paymentIdUuid)
                .userId(userIdUuid)
                .subscriptionMonths(1)
                .amount(10000)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findById(paymentIdUuid)).thenReturn(Optional.of(payment));

        // Act
        PaymentDto result = paymentService.getPayment(paymentId, userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(paymentIdUuid);
        assertThat(result.getUserId()).isEqualTo(userIdUuid);
        assertThat(result.getAmount()).isEqualTo(10000);
    }

    @Test
    void getPayment_WrongUser_ThrowsException() {
        // Arrange
        String wrongUserId = "wrong-user-789";
        Payment payment = Payment.builder()
                .id(paymentIdUuid)
                .userId(userIdUuid)  // Different user
                .build();

        when(paymentRepository.findById(paymentIdUuid)).thenReturn(Optional.of(payment));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.getPayment(paymentId, wrongUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void getPayment_NotFound_ThrowsException() {
        // Arrange
        String nonExistentPaymentId = "non-existent-payment";
        UUID nonExistentUuid = UUID.fromString(nonExistentPaymentId);
        when(paymentRepository.findById(nonExistentUuid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> paymentService.getPayment(nonExistentPaymentId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found");
    }
}