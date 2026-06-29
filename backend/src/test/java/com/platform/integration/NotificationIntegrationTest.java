package com.platform.integration;

import com.platform.model.Notification;
import com.platform.model.User;
import com.platform.repository.NotificationRepository;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Notification endpoints.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;

    private String userId1;
    private org.springframework.security.oauth2.jwt.Jwt jwt1;
    private org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken auth1;

    @BeforeEach
    void setUp() {
        userId1 = userId();
        jwt1 = createJwt(userId1, "notif-" + userId1.substring(0, 8), "USER_0");
        auth1 = createAuth(jwt1, "USER_0");

        userRepository.save(User.builder().id(UUID.fromString(userId1))
                .username("notif-" + userId1.substring(0, 8)).role("USER_0").version(0L).build());
    }

    private Notification createNotification(String userId, boolean read) {
        return notificationRepository.save(Notification.builder()
                .userId(userId)
                .type("event")
                .title("Test notification")
                .message("You have a new event")
                .read(read)
                .build());
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/notifications — list all notifications")
    void getAllNotifications() throws Exception {
        createNotification(userId1, false);
        createNotification(userId1, true);

        mockMvc.perform(get("/api/notifications")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/notifications/unread — only unread")
    void getUnreadNotifications() throws Exception {
        createNotification(userId1, false);
        createNotification(userId1, true);
        createNotification(userId1, false);

        mockMvc.perform(get("/api/notifications/unread")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @Order(3)
    @DisplayName("PUT /api/notifications/{id}/read — mark single as read")
    void markAsRead() throws Exception {
        Notification notif = createNotification(userId1, false);

        mockMvc.perform(put("/api/notifications/{id}/read", notif.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isNoContent());

        // Verify it's now read
        mockMvc.perform(get("/api/notifications/unread")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Order(4)
    @DisplayName("PUT /api/notifications/read-all — mark all as read")
    void markAllAsRead() throws Exception {
        createNotification(userId1, false);
        createNotification(userId1, false);
        createNotification(userId1, false);

        mockMvc.perform(put("/api/notifications/read-all")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications/unread")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Order(5)
    @DisplayName("Notifications are user-scoped — different users see different data")
    void userScoped() throws Exception {
        String userId2 = userId();
        var jwt2 = createJwt(userId2, "notif2-" + userId2.substring(0, 8), "USER_0");
        var auth2 = createAuth(jwt2, "USER_0");
        userRepository.save(User.builder().id(UUID.fromString(userId2))
                .username("notif2-" + userId2.substring(0, 8)).role("USER_0").version(0L).build());

        createNotification(userId1, false);
        createNotification(userId2, false);

        mockMvc.perform(get("/api/notifications/unread")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/notifications/sync — sync notification states")
    void syncNotifications() throws Exception {
        Notification notif = createNotification(userId1, false);

        var syncBody = java.util.List.of(
                java.util.Map.of("id", notif.getId(), "read", true)
        );

        mockMvc.perform(post("/api/notifications/sync")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(syncBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").value(1));
    }
}
