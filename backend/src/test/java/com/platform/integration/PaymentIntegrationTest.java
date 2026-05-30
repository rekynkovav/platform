package com.platform.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.model.dto.PaymentDto;
import com.platform.model.entity.User;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;
    private String user1Id;
    private String user2Id;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser1 = User.builder()
                .id(UUID.randomUUID())
                .username("payment_user1")
                .email("payment1@example.com")
                .passwordHash("hashed_password_1")
                .createdAt(Instant.now())
                .build();

        testUser2 = User.builder()
                .id(UUID.randomUUID())
                .username("payment_user2")
                .email("payment2@example.com")
                .passwordHash("hashed_password_2")
                .createdAt(Instant.now())
                .build();

        userRepository.save(testUser1);
        userRepository.save(testUser2);

        user1Id = testUser1.getId().toString();
        user2Id = testUser2.getId().toString();
    }

    @Test
    void createPayment_ValidRequest_ReturnsPaymentDto() throws Exception {
        // Arrange
        PaymentDto request = PaymentDto.builder()
                .subscriptionMonths(1)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user1Id))
                .andExpect(jsonPath("$.subscriptionMonths").value(1))
                .andExpect(jsonPath("$.amount").value(10000)) // 1 month = 10000
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.qrCodeData").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createPayment_ThreeMonths_ReturnsDiscountedAmount() throws Exception {
        // Arrange
        PaymentDto request = PaymentDto.builder()
                .subscriptionMonths(3)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionMonths").value(3))
                .andExpect(jsonPath("$.amount").value(27000)) // 3 * 10000 * 0.9 = 27000
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createPayment_SixMonths_ReturnsDiscountedAmount() throws Exception {
        // Arrange
        PaymentDto request = PaymentDto.builder()
                .subscriptionMonths(6)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionMonths").value(6))
                .andExpect(jsonPath("$.amount").value(54000)) // 6 * 10000 * 0.9 = 54000
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createPayment_InvalidMonths_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentDto request = PaymentDto.builder()
                .subscriptionMonths(0) // Invalid: must be 1, 3, or 6
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_NoUserId_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentDto request = PaymentDto.builder()
                .subscriptionMonths(1)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_ExistingPendingPayment_ReturnsBadRequest() throws Exception {
        // Arrange
        // Create first pending payment
        PaymentDto request1 = PaymentDto.builder()
                .subscriptionMonths(1)
                .build();

        mockMvc.perform(post("/api/payments")
                .header("X-User-Id", user1Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)));

        // Try to create second pending payment
        PaymentDto request2 = PaymentDto.builder()
                .subscriptionMonths(3)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPayment_ValidRequest_ReturnsPaymentDto() throws Exception {
        // Arrange
        // First create a payment
        PaymentDto createRequest = PaymentDto.builder()
                .subscriptionMonths(1)
                .build();

        String createResponse = mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PaymentDto createdPayment = objectMapper.readValue(createResponse, PaymentDto.class);
        String paymentId = createdPayment.getId().toString();

        // Act & Assert - Get the payment
        mockMvc.perform(get("/api/payments/{id}", paymentId)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.userId").value(user1Id))
                .andExpect(jsonPath("$.subscriptionMonths").value(1))
                .andExpect(jsonPath("$.amount").value(10000))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getPayment_NonExistentPayment_ReturnsNotFound() throws Exception {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();

        // Act & Assert
        mockMvc.perform(get("/api/payments/{id}", nonExistentId)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPayment_NotOwner_ReturnsForbidden() throws Exception {
        // Arrange
        // User1 creates a payment
        PaymentDto createRequest = PaymentDto.builder()
                .subscriptionMonths(1)
                .build();

        String createResponse = mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PaymentDto createdPayment = objectMapper.readValue(createResponse, PaymentDto.class);
        String paymentId = createdPayment.getId().toString();

        // Act & Assert - User2 tries to get the payment
        mockMvc.perform(get("/api/payments/{id}", paymentId)
                        .header("X-User-Id", user2Id))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmPayment_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        // Create a payment
        PaymentDto createRequest = PaymentDto.builder()
                .subscriptionMonths(1)
                .build();

        String createResponse = mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PaymentDto createdPayment = objectMapper.readValue(createResponse, PaymentDto.class);
        String paymentId = createdPayment.getId().toString();

        // Act & Assert - Confirm the payment
        mockMvc.perform(post("/api/payments/{id}/confirm", paymentId)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isOk());

        // Verify payment is confirmed
        mockMvc.perform(get("/api/payments/{id}", paymentId)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedAt").exists())
                .andExpect(jsonPath("$.qrCodeData").doesNotExist()); // Should be cleared
    }

    @Test
    void confirmPayment_AlreadyConfirmed_ReturnsBadRequest() throws Exception {
        // Arrange
        // Create and confirm a payment
        PaymentDto createRequest = PaymentDto.builder()
                .subscriptionMonths(1)
                .build();

        String createResponse = mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PaymentDto createdPayment = objectMapper.readValue(createResponse, PaymentDto.class);
        String paymentId = createdPayment.getId().toString();

        // Confirm once
        mockMvc.perform(post("/api/payments/{id}/confirm", paymentId)
                .header("X-User-Id", user1Id));

        // Act & Assert - Try to confirm again
        mockMvc.perform(post("/api/payments/{id}/confirm", paymentId)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmPayment_ExpiredPayment_ReturnsBadRequest() throws Exception {
        // Arrange
        // Create a payment
        PaymentDto createRequest = PaymentDto.builder()
                .subscriptionMonths(1)
                .build();

        String createResponse = mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PaymentDto createdPayment = objectMapper.readValue(createResponse, PaymentDto.class);
        String paymentId = createdPayment.getId().toString();

        // Simulate expiration (in real scenario, this would happen via scheduled task)
        // For test, we'll just verify the endpoint exists
        mockMvc.perform(post("/api/payments/expire-old")
                .header("X-User-Id", user1Id));

        // Act & Assert - Try to confirm expired payment
        mockMvc.perform(post("/api/payments/{id}/confirm", paymentId)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmPayment_NotOwner_ReturnsForbidden() throws Exception {
        // Arrange
        // User1 creates a payment
        PaymentDto createRequest = PaymentDto.builder()
                .subscriptionMonths(1)
                .build();

        String createResponse = mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PaymentDto createdPayment = objectMapper.readValue(createResponse, PaymentDto.class);
        String paymentId = createdPayment.getId().toString();

        // Act & Assert - User2 tries to confirm
        mockMvc.perform(post("/api/payments/{id}/confirm", paymentId)
                        .header("X-User-Id", user2Id))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserPayments_ValidRequest_ReturnsPayments() throws Exception {
        // Arrange
        // Create multiple payments for user1
        for (int i = 1; i <= 3; i++) {
            PaymentDto payment = PaymentDto.builder()
                    .subscriptionMonths(1)
                    .build();
            // Need to handle pending payment constraint - confirm previous one first
            if (i > 1) {
                // Get previous payment ID and confirm it
                String previousResponse = mockMvc.perform(get("/api/payments/user/{userId}", user1Id)
                                .header("X-User-Id", user1Id))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                // Parse response to get latest payment ID
                String previousPaymentId = objectMapper.readTree(previousResponse)
                        .get(0)
                        .get("id")
                        .asText();

                mockMvc.perform(post("/api/payments/{id}/confirm", previousPaymentId)
                        .header("X-User-Id", user1Id));
            }

            mockMvc.perform(post("/api/payments")
                    .header("X-User-Id", user1Id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payment)));
        }

        // Act & Assert - Get user1's payments
        mockMvc.perform(get("/api/payments/user/{userId}", user1Id)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].userId").value(user1Id))
                .andExpect(jsonPath("$[1].userId").value(user1Id))
                .andExpect(jsonPath("$[2].userId").value(user1Id));
    }

    @Test
    void getUserPayments_WithPagination_ReturnsPage() throws Exception {
        // Arrange
        // Create multiple payments for user1 (with confirmations in between)
        for (int i = 1; i <= 7; i++) {
            PaymentDto payment = PaymentDto.builder()
                    .subscriptionMonths(1)
                    .build();

            if (i > 1) {
                // Confirm previous payment
                String previousResponse = mockMvc.perform(get("/api/payments/user/{userId}", user1Id)
                                .header("X-User-Id", user1Id))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                String previousPaymentId = objectMapper.readTree(previousResponse)
                        .get(0)
                        .get("id")
                        .asText();

                mockMvc.perform(post("/api/payments/{id}/confirm", previousPaymentId)
                        .header("X-User-Id", user1Id));
            }

            mockMvc.perform(post("/api/payments")
                    .header("X-User-Id", user1Id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payment)));
        }

        // Act & Assert - Get first page
        mockMvc.perform(get("/api/payments/user/{userId}", user1Id)
                        .header("X-User-Id", user1Id)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void getPaymentStatus_ValidRequest_ReturnsStatus() throws Exception {
        // Arrange
        // Create a payment
        PaymentDto createRequest = PaymentDto.builder()
                .subscriptionMonths(1)
                .build();

        String createResponse = mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PaymentDto createdPayment = objectMapper.readValue(createResponse, PaymentDto.class);
        String paymentId = createdPayment.getId().toString();

        // Act & Assert
        mockMvc.perform(get("/api/payments/{id}/status", paymentId)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isOk())
                .andExpect(content().string("PENDING"));
    }

    @Test
    void getPaymentQRCode_ValidRequest_ReturnsQRCode() throws Exception {
        // Arrange
        // Create a payment
        PaymentDto createRequest = PaymentDto.builder()
                .subscriptionMonths(1)
                .build();

        String createResponse = mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PaymentDto createdPayment = objectMapper.readValue(createResponse, PaymentDto.class);
        String paymentId = createdPayment.getId().toString();

        // Act & Assert
        mockMvc.perform(get("/api/payments/{id}/qrcode", paymentId)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().string(createdPayment.getQrCodeData()));
    }

    @Test
    void getPaymentQRCode_ConfirmedPayment_ReturnsNotFound() throws Exception {
        // Arrange
        // Create and confirm a payment
        PaymentDto createRequest = PaymentDto.builder()
                .subscriptionMonths(1)
                .build();

        String createResponse = mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PaymentDto createdPayment = objectMapper.readValue(createResponse, PaymentDto.class);
        String paymentId = createdPayment.getId().toString();

        // Confirm the payment
        mockMvc.perform(post("/api/payments/{id}/confirm", paymentId)
                .header("X-User-Id", user1Id));

        // Act & Assert - QR code should be cleared after confirmation
        mockMvc.perform(get("/api/payments/{id}/qrcode", paymentId)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isNotFound());
    }

    @Test
    void expireOldPayments_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        // Create a payment (it will be old enough to expire in test context)
        PaymentDto createRequest = PaymentDto.builder()
                .subscriptionMonths(1)
                .build();

        String createResponse = mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PaymentDto createdPayment = objectMapper.readValue(createResponse, PaymentDto.class);
        String paymentId = createdPayment.getId().toString();

        // Act & Assert
        mockMvc.perform(post("/api/payments/expire-old")
                        .header("X-User-Id", user1Id))
                .andExpect(status().isOk());

        // Verify payment is expired
        mockMvc.perform(get("/api/payments/{id}/status", paymentId)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isOk())
                .andExpect(content().string("EXPIRED"));
    }
}