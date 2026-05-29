package com.platform.service;

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

    @Mock
    private Jwt jwt;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;
    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "user-123";
        testPayment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .amount(new BigDecimal("100.00"))
                .status(Payment.PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreatePayment_ValidSubscription_CreatesPayment() {
        // Arrange
        when(jwt.getSubject()).thenReturn(testUserId);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        Payment result = paymentService.createPayment(1, jwt);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUserId);
        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void testCreatePayment_InvalidSubscriptionMonths_ThrowsException() {
        // Arrange
        when(jwt.getSubject()).thenReturn(testUserId);

        // Act & Assert
        try {
            paymentService.createPayment(0, jwt);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("subscriptionMonths");
        }

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testFindByUserIdAndStatus_ReturnsPayments() {
        // Arrange
        List<Payment> payments = List.of(testPayment);
        when(paymentRepository.findByUserIdAndStatus(testUserId, Payment.PaymentStatus.PENDING))
                .thenReturn(payments);

        // Act
        List<Payment> result = paymentService.findByUserIdAndStatus(testUserId, Payment.PaymentStatus.PENDING);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(testUserId);
        assertThat(result.get(0).getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
        verify(paymentRepository).findByUserIdAndStatus(testUserId, Payment.PaymentStatus.PENDING);
    }

    @Test
    void testFindByUserIdAndStatus_NoPayments_ReturnsEmptyList() {
        // Arrange
        when(paymentRepository.findByUserIdAndStatus(testUserId, Payment.PaymentStatus.COMPLETED))
                .thenReturn(List.of());

        // Act
        List<Payment> result = paymentService.findByUserIdAndStatus(testUserId, Payment.PaymentStatus.COMPLETED);

        // Assert
        assertThat(result).isEmpty();
        verify(paymentRepository).findByUserIdAndStatus(testUserId, Payment.PaymentStatus.COMPLETED);
    }

    @Test
    void testGetPaymentById_ReturnsPayment() {
        // Arrange
        UUID paymentId = testPayment.getId();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));

        // Act
        Optional<Payment> result = paymentService.getPaymentById(paymentId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(paymentId);
        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void testGetPaymentById_NonExistent_ReturnsEmpty() {
        // Arrange
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // Act
        Optional<Payment> result = paymentService.getPaymentById(paymentId);

        // Assert
        assertThat(result).isEmpty();
        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void testUpdatePaymentStatus_ValidTransition_UpdatesStatus() {
        // Arrange
        UUID paymentId = testPayment.getId();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        Payment result = paymentService.updatePaymentStatus(paymentId, Payment.PaymentStatus.COMPLETED);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void testUpdatePaymentStatus_InvalidTransition_ThrowsException() {
        // Arrange
        UUID paymentId = testPayment.getId();
        testPayment.setStatus(Payment.PaymentStatus.COMPLETED); // Уже завершен
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));

        // Act & Assert
        try {
            paymentService.updatePaymentStatus(paymentId, Payment.PaymentStatus.PENDING);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("Invalid status transition");
        }

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testProcessPayment_ValidPayment_ProcessesSuccessfully() {
        // Arrange
        UUID paymentId = testPayment.getId();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        Payment result = paymentService.processPayment(paymentId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void testCancelPayment_CancelsPayment() {
        // Arrange
        UUID paymentId = testPayment.getId();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        Payment result = paymentService.cancelPayment(paymentId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.CANCELLED);
        verify(paymentRepository).save(any(Payment.class));
    }
}