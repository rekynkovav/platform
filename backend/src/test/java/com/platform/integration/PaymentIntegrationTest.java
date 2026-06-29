package com.platform.integration;

import com.platform.model.Payment;
import com.platform.model.User;
import com.platform.repository.PaymentRepository;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Payment endpoints.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentIntegrationTest extends AbstractIntegrationTest {

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private UserRepository userRepository;

    private String userId1;
    private Jwt jwt1;
    private JwtAuthenticationToken auth1;
    private String adminUserId;
    private Jwt adminJwt;
    private JwtAuthenticationToken adminAuth;

    @BeforeEach
    void setUp() {
        userId1 = userId();
        jwt1 = createJwt(userId1, "pay-" + userId1.substring(0, 8), "USER_0");
        auth1 = createAuth(jwt1, "USER_0");

        adminUserId = userId();
        adminJwt = createJwt(adminUserId, "pay-admin-" + adminUserId.substring(0, 8), "SUPER_ADMIN");
        adminAuth = createAuth(adminJwt, "SUPER_ADMIN");

        userRepository.save(User.builder().id(UUID.fromString(userId1))
                .username("pay-" + userId1.substring(0, 8)).role("USER_0").version(0L).build());
        userRepository.save(User.builder().id(UUID.fromString(adminUserId))
                .username("pay-admin-" + adminUserId.substring(0, 8)).role("SUPER_ADMIN").version(0L).build());
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/payments/my — list user's payments (empty)")
    void getMyPayments() throws Exception {
        mockMvc.perform(get("/api/payments/my")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/payments/user — alias for my payments")
    void getUserPaymentsAlias() throws Exception {
        mockMvc.perform(get("/api/payments/user")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/payments/pending — admin sees pending payments")
    void getPendingPayments() throws Exception {
        paymentRepository.save(Payment.builder()
                .id(UUID.randomUUID()).userId(UUID.fromString(userId1))
                .amount(BigDecimal.valueOf(990)).currency("RUB")
                .subscriptionMonths(1).status(Payment.PaymentStatus.PENDING)
                .paymentMethod("SBP").build());

        mockMvc.perform(get("/api/payments/pending")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/payments/pending — 403 for regular user")
    void getPendingForbidden() throws Exception {
        mockMvc.perform(get("/api/payments/pending")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/payments/{id} — get payment by ID")
    void getPaymentById() throws Exception {
        Payment payment = paymentRepository.save(Payment.builder()
                .id(UUID.randomUUID()).userId(UUID.fromString(userId1))
                .amount(BigDecimal.valueOf(1990)).currency("RUB")
                .subscriptionMonths(3).status(Payment.PaymentStatus.PENDING)
                .paymentMethod("SBP").build());

        mockMvc.perform(get("/api/payments/{paymentId}", payment.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1990))
                .andExpect(jsonPath("$.subscriptionMonths").value(3));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/payments/{id}/status — check payment status")
    void checkPaymentStatus() throws Exception {
        Payment payment = paymentRepository.save(Payment.builder()
                .id(UUID.randomUUID()).userId(UUID.fromString(userId1))
                .amount(BigDecimal.valueOf(990)).currency("RUB")
                .subscriptionMonths(1).status(Payment.PaymentStatus.PENDING)
                .paymentMethod("SBP").build());

        mockMvc.perform(get("/api/payments/{paymentId}/status", payment.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/payments/{paymentId}/confirm — admin confirms payment")
    void confirmPayment() throws Exception {
        Payment payment = paymentRepository.save(Payment.builder()
                .id(UUID.randomUUID()).userId(UUID.fromString(userId1))
                .amount(BigDecimal.valueOf(990)).currency("RUB")
                .subscriptionMonths(1).status(Payment.PaymentStatus.PENDING)
                .paymentMethod("SBP").build());

        mockMvc.perform(post("/api/payments/{paymentId}/confirm", payment.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Confirmed via test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @Order(8)
    @DisplayName("POST /api/payments/{paymentId}/confirm — 403 for regular user")
    void confirmPaymentForbidden() throws Exception {
        Payment payment = paymentRepository.save(Payment.builder()
                .id(UUID.randomUUID()).userId(UUID.fromString(userId1))
                .amount(BigDecimal.valueOf(990)).currency("RUB")
                .subscriptionMonths(1).status(Payment.PaymentStatus.PENDING)
                .paymentMethod("SBP").build());

        mockMvc.perform(post("/api/payments/{paymentId}/confirm", payment.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"hack\"}"))
                .andExpect(status().isForbidden());
    }
}
