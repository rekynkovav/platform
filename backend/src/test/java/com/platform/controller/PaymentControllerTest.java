package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.exception.GlobalExceptionHandler;
import com.platform.model.Payment;
import com.platform.model.dto.PaymentDto;
import com.platform.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.platform.controller.TestSecurityHelper.securityContextFilter;
import static com.platform.controller.TestSecurityHelper.withAuth;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private PaymentService paymentService;
    @InjectMocks private PaymentController paymentController;

    private String userId;
    private String paymentId;
    private Jwt userJwt;
    private Jwt adminJwt;
    private JwtAuthenticationToken userAuth;
    private JwtAuthenticationToken adminAuth;
    private Payment payment;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .addFilters(securityContextFilter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        userId = UUID.randomUUID().toString();
        paymentId = UUID.randomUUID().toString();

        userJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("preferred_username", "user")
                .claim("realm_access.roles", "USER_0")
                .build();

        adminJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", UUID.randomUUID().toString())
                .claim("preferred_username", "admin")
                .claim("realm_access.roles", "SUPER_ADMIN")
                .build();

        userAuth = new JwtAuthenticationToken(userJwt, List.of(new SimpleGrantedAuthority("ROLE_USER_0")));
        adminAuth = new JwtAuthenticationToken(adminJwt, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));

        payment = Payment.builder()
                .id(UUID.fromString(paymentId))
                .userId(UUID.fromString(userId))
                .amount(new BigDecimal("999.00"))
                .currency("RUB")
                .subscriptionMonths(1)
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod("SBP")
                .qrCodeData("https://qr.nspk.ru/abc123")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== POST /api/payments/create ====================

    @Test
    void createPayment_UserCanCreate_ReturnsCreated() throws Exception {
        PaymentDto.CreateRequest request = new PaymentDto.CreateRequest();
        request.setSubscriptionMonths(1);

        when(paymentService.createPayment(eq(1), any(Jwt.class))).thenReturn(payment);

        mockMvc.perform(post("/api/payments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(userAuth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(999.00))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // ==================== POST /api/payments (alias) ====================

    @Test
    void createPaymentAlias_DelegatesToCreate() throws Exception {
        PaymentDto.CreateRequest request = new PaymentDto.CreateRequest();
        request.setSubscriptionMonths(3);

        when(paymentService.createPayment(eq(3), any(Jwt.class))).thenReturn(payment);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(userAuth)))
                .andExpect(status().isCreated());
    }

    // ==================== GET /api/payments/{paymentId} ====================

    @Test
    void getPayment_OwnerCanView() throws Exception {
        when(paymentService.getPayment(eq(paymentId), any(Jwt.class))).thenReturn(payment);

        mockMvc.perform(get("/api/payments/{paymentId}", paymentId)
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.currency").value("RUB"));
    }

    // ==================== GET /api/payments/{paymentId}/qr ====================

    @Test
    void getPaymentQR_ReturnsQrData() throws Exception {
        when(paymentService.getPayment(eq(paymentId), any(Jwt.class))).thenReturn(payment);

        mockMvc.perform(get("/api/payments/{paymentId}/qr", paymentId)
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrCodeData").exists());
    }

    // ==================== GET /api/payments/my ====================

    @Test
    void getMyPayments_ReturnsList() throws Exception {
        when(paymentService.getUserPayments(any(Jwt.class), eq(50))).thenReturn(List.of(payment));

        mockMvc.perform(get("/api/payments/my")
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].paymentMethod").value("SBP"));
    }

    // ==================== GET /api/payments/user (alias) ====================

    @Test
    void getUserPaymentsAlias_DelegatesToMyPayments() throws Exception {
        when(paymentService.getUserPayments(any(Jwt.class), eq(50))).thenReturn(List.of());

        mockMvc.perform(get("/api/payments/user")
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ==================== GET /api/payments/{paymentId}/status ====================

    @Test
    void checkPaymentStatus_ReturnsUpdatedStatus() throws Exception {
        Payment confirmed = Payment.builder()
                .id(UUID.fromString(paymentId)).userId(UUID.fromString(userId))
                .amount(new BigDecimal("999.00")).currency("RUB")
                .status(Payment.PaymentStatus.CONFIRMED)
                .paidAt(LocalDateTime.now()).createdAt(LocalDateTime.now()).build();

        when(paymentService.checkPaymentStatus(eq(paymentId), any(Jwt.class))).thenReturn(confirmed);

        mockMvc.perform(get("/api/payments/{paymentId}/status", paymentId)
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    // ==================== POST /api/payments/{paymentId}/confirm ====================

    @Test
    void confirmPayment_AdminCanConfirm() throws Exception {
        PaymentDto.ConfirmRequest request = new PaymentDto.ConfirmRequest();
        request.setNotes("Verified via bank statement");

        Payment confirmed = Payment.builder()
                .id(UUID.fromString(paymentId)).userId(UUID.fromString(userId))
                .amount(new BigDecimal("999.00")).status(Payment.PaymentStatus.CONFIRMED)
                .paidAt(LocalDateTime.now()).createdAt(LocalDateTime.now()).build();

        when(paymentService.confirmPayment(eq(paymentId), eq("Verified via bank statement"), any(Jwt.class)))
                .thenReturn(confirmed);

        mockMvc.perform(post("/api/payments/{paymentId}/confirm", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    // ==================== GET /api/payments/pending ====================

    @Test
    void getPendingPayments_AdminCanView() throws Exception {
        when(paymentService.getPendingPayments(50)).thenReturn(List.of(payment));

        mockMvc.perform(get("/api/payments/pending")
                        .with(withAuth(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }
}
