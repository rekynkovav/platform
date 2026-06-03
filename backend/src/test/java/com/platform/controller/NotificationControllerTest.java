package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.exception.GlobalExceptionHandler;
import com.platform.model.Notification;
import com.platform.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.platform.controller.TestSecurityHelper.securityContextFilter;
import static com.platform.controller.TestSecurityHelper.withAuth;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private String userId;
    private Jwt jwt;
    private JwtAuthenticationToken auth;
    private Notification notification;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .addFilters(securityContextFilter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        userId = UUID.randomUUID().toString();

        jwt = Jwt.withTokenValue("test-token-value")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("preferred_username", "testuser")
                .claim("realm_access.roles", "USER_0")
                .build();

        auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER_0")));

        notification = Notification.builder()
                .id(1L)
                .userId(userId)
                .type("new_message")
                .title("New Message")
                .message("Hello!")
                .read(false)
                .createdAt(Instant.now())
                .build();
    }

    // ==================== GET /api/notifications ====================

    @Test
    void getAll_ReturnsList() throws Exception {
        when(notificationService.getAllForUser(userId)).thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications")
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("New Message"));
    }

    @Test
    void getAll_Empty_ReturnsEmptyArray() throws Exception {
        when(notificationService.getAllForUser(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/notifications")
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ==================== GET /api/notifications/unread ====================

    @Test
    void getUnread_ReturnsUnreadOnly() throws Exception {
        when(notificationService.getUnreadForUser(userId)).thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications/unread")
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].read").value(false));
    }

    // ==================== PUT /api/notifications/{id}/read ====================

    @Test
    void markAsRead_Returns204() throws Exception {
        doNothing().when(notificationService).markAsReadForUser(1L, userId);

        mockMvc.perform(put("/api/notifications/{id}/read", 1L)
                        .with(withAuth(auth)))
                .andExpect(status().isNoContent());

        verify(notificationService).markAsReadForUser(1L, userId);
    }

    // ==================== PUT /api/notifications/read-all ====================

    @Test
    void markAllAsRead_Returns204() throws Exception {
        doNothing().when(notificationService).markAllAsRead(userId);

        mockMvc.perform(put("/api/notifications/read-all")
                        .with(withAuth(auth)))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead(userId);
    }

    // ==================== POST /api/notifications/sync ====================

    @Test
    void syncNotifications_ValidPayload_ReturnsSyncedCount() throws Exception {
        doNothing().when(notificationService).markAsReadForUser(anyLong(), eq(userId));

        List<Map<String, Object>> payload = List.of(
                Map.of("id", 1, "read", true),
                Map.of("id", 2, "read", true),
                Map.of("id", 3, "read", false)
        );

        mockMvc.perform(post("/api/notifications/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").value(2))
                .andExpect(jsonPath("$.total").value(3));

        verify(notificationService, times(2)).markAsReadForUser(anyLong(), eq(userId));
    }

    @Test
    void syncNotifications_EmptyPayload_ReturnsZeroSynced() throws Exception {
        List<Map<String, Object>> payload = List.of();

        mockMvc.perform(post("/api/notifications/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void syncNotifications_NonNumericId_SkipsGracefully() throws Exception {
        List<Map<String, Object>> payload = List.of(
                Map.of("id", "client-uuid-123", "read", true)
        );

        mockMvc.perform(post("/api/notifications/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").value(0))
                .andExpect(jsonPath("$.total").value(1));

        verify(notificationService, never()).markAsReadForUser(anyLong(), anyString());
    }
}
