package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.model.dto.PaymentDto;
import com.platform.model.enums.PaymentStatus;
import com.platform.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private String userId;
    private UUID userIdUuid;
    private String paymentId;
    private UUID paymentIdUuid;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
        objectMapper = new ObjectMapper();

        userId = "user-123";
        userIdUuid = UUID.fromString(userId);
        paymentId = "payment-456";
        paymentIdUuid = UUID.fromString(paymentId);
    }

    @Test
    void createPayment_ValidRequest_ReturnsPaymentDto() throws Exception {
        // Arrange
        int months = 3;
        PaymentDto expectedResponse = PaymentDto.builder()
                .id(paymentIdUuid)
                .userId(userIdUuid)
                .subscriptionMonths(months)
                .amount(27000)  // 3 * 10000 * 0.9 = 27000 (10% discount)
                .status(PaymentStatus.PENDING)
                .qrCodeData("qr-code-base64-data")
                .createdAt(Instant.now())
                .build();

        when(paymentService.createPayment(eq(userId), eq(months)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"months\": " + months + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.subscriptionMonths").value(months))
                .andExpect(jsonPath("$.amount").value(27000))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.qrCodeData").value("qr-code-base64-data"));
    }

    @Test
    void createPayment_InvalidMonths_ReturnsBadRequest() throws Exception {
        // Arrange
        int invalidMonths = 0;

        when(paymentService.createPayment(eq(userId), eq(invalidMonths)))
                .thenThrow(new IllegalArgumentException("Invalid subscription months"));

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"months\": " + invalidMonths + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_ExistingPendingPayment_ReturnsConflict() throws Exception {
        // Arrange
        int months = 1;

        when(paymentService.createPayment(eq(userId), eq(months)))
                .thenThrow(new com.platform.exception.BusinessException("User already has a pending payment"));

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"months\": " + months + "}"))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmPayment_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        when(paymentService.confirmPayment(eq(paymentId))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/payments/{id}/confirm", paymentId))
                .andExpect(status().isOk());
    }

    @Test
    void confirmPayment_AlreadyProcessed_ReturnsBadRequest() throws Exception {
        // Arrange
        when(paymentService.confirmPayment(eq(paymentId)))
                .thenThrow(new IllegalStateException("Payment already processed"));

        // Act & Assert
        mockMvc.perform(post("/api/payments/{id}/confirm", paymentId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmPayment_NotFound_ReturnsNotFound() throws Exception {
        // Arrange
        String nonExistentPaymentId = "non-existent-payment";
        when(paymentService.confirmPayment(eq(nonExistentPaymentId)))
                .thenThrow(new com.platform.exception.ResourceNotFoundException("Payment not found"));

        // Act & Assert
        mockMvc.perform(post("/api/payments/{id}/confirm", nonExistentPaymentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPayment_ValidRequest_ReturnsPaymentDto() throws Exception {
        // Arrange
        PaymentDto expectedResponse = PaymentDto.builder()
                .id(paymentIdUuid)
                .userId(userIdUuid)
                .subscriptionMonths(1)
                .amount(10000)
                .status(PaymentStatus.PENDING)
                .qrCodeData("qr-code-base64-data")
                .createdAt(Instant.now().minusMinutes(30))
                .build();

        when(paymentService.getPayment(eq(paymentId), eq(userId)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/payments/{id}", paymentId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.amount").value(10000));
    }

    @Test
    void getPayment_WrongUser_ReturnsForbidden() throws Exception {
        // Arrange
        String wrongUserId = "wrong-user-789";
        when(paymentService.getPayment(eq(paymentId), eq(wrongUserId)))
                .thenThrow(new com.platform.exception.AccessDeniedException("Access denied"));

        // Act & Assert
        mockMvc.perform(get("/api/payments/{id}", paymentId)
                        .header("X-User-Id", wrongUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserPayments_ValidRequest_ReturnsPayments() throws Exception {
        // Arrange
        PaymentDto payment1 = PaymentDto.builder()
                .id(UUID.randomUUID())
                .userId(userIdUuid)
                .subscriptionMonths(1)
                .amount(10000)
                .status(PaymentStatus.CONFIRMED)
                .confirmedAt(Instant.now().minusDays(5))
                .createdAt(Instant.now().minusDays(6))
                .build();

        PaymentDto payment2 = PaymentDto.builder()
                .id(UUID.randomUUID())
                .userId(userIdUuid)
                .subscriptionMonths(3)
                .amount(27000)
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now().minusHours(2))
                .build();

        List<PaymentDto> expectedResponse = List.of(payment1, payment2);

        when(paymentService.findByUserIdAndStatus(eq(userId), isNull()))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/payments/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[1].userId").value(userId));
    }

    @Test
    void getUserPayments_WithStatusFilter_ReturnsFilteredPayments() throws Exception {
        // Arrange
        PaymentStatus status = PaymentStatus.CONFIRMED;
        PaymentDto payment = PaymentDto.builder()
                .id(UUID.randomUUID())
                .userId(userIdUuid)
                .subscriptionMonths(1)
                .amount(10000)
                .status(status)
                .confirmedAt(Instant.now().minusDays(3))
                .createdAt(Instant.now().minusDays(4))
                .build();

        List<PaymentDto> expectedResponse = List.of(payment);

        when(paymentService.findByUserIdAndStatus(eq(userId), eq(status)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/payments/user/{userId}", userId)
                        .param("status", status.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void getUserPayments_NoPayments_ReturnsEmptyList() throws Exception {
        // Arrange
        when(paymentService.findByUserIdAndStatus(eq(userId), isNull()))
                .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/payments/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getPaymentStatus_ValidRequest_ReturnsStatus() throws Exception {
        // Arrange
        PaymentDto expectedResponse = PaymentDto.builder()
                .id(paymentIdUuid)
                .userId(userIdUuid)
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now().minusMinutes(30))
                .build();

        when(paymentService.getPayment(eq(paymentId), eq(userId)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/payments/{id}/status", paymentId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getPaymentStatus_WrongUser_ReturnsForbidden() throws Exception {
        // Arrange
        String wrongUserId = "wrong-user-789";
        when(paymentService.getPayment(eq(paymentId), eq(wrongUserId)))
                .thenThrow(new com.platform.exception.AccessDeniedException("Access denied"));

        // Act & Assert
        mockMvc.perform(get("/api/payments/{id}/status", paymentId)
                        .header("X-User-Id", wrongUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPaymentQRCode_ValidRequest_ReturnsQRCode() throws Exception {
        // Arrange
        PaymentDto expectedResponse = PaymentDto.builder()
                .id(paymentIdUuid)
                .userId(userIdUuid)
                .status(PaymentStatus.PENDING)
                .qrCodeData("qr-code-base64-data")
                .createdAt(Instant.now().minusMinutes(30))
                .build();

        when(paymentService.getPayment(eq(paymentId), eq(userId)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/payments/{id}/qrcode", paymentId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrCodeData").value("qr-code-base64-data"));
    }

    @Test
    void getPaymentQRCode_ConfirmedPayment_ReturnsNoQRCode() throws Exception {
        // Arrange
        PaymentDto expectedResponse = PaymentDto.builder()
                .id(paymentIdUuid)
                .userId(userIdUuid)
                .status(PaymentStatus.CONFIRMED)
                .qrCodeData(null)  // No QR code for confirmed payments
                .confirmedAt(Instant.now().minusMinutes(10))
                .createdAt(Instant.now().minusHours(1))
                .build();

        when(paymentService.getPayment(eq(paymentId), eq(userId)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/payments/{id}/qrcode", paymentId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrCodeData").doesNotExist());
    }

    @Test
    void expireOldPayments_AdminRequest_ReturnsOk() throws Exception {
        // Arrange
        String adminUserId = "admin-001";
        when(paymentService.expireOldPayments()).thenReturn(5);

        // Act & Assert
        mockMvc.perform(post("/api/payments/expire-old")
                        .header("X-User-Id", adminUserId))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void expireOldPayments_RegularUser_ReturnsForbidden() throws Exception {
        // Arrange
        // This endpoint should check for admin role
        // For simplicity, we'll assume it returns 403 for non-admins

        // Act & Assert
        mockMvc.perform(post("/api/payments/expire-old")
                        .header("X-User-Id", userId))
                .andExpect(status().isForbidden());
    }
}