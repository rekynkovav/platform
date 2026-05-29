package com.platform.controller;

import com.platform.config.TestSecurityConfig;
import com.platform.model.Notification;
import com.platform.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import(TestSecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    private Notification createTestNotification() {
        return Notification.builder()
                .id("test-id")
                .userId("user-123")
                .type("info")
                .title("Test Title")
                .message("Test Message")
                .read(false)
                .build();
    }

    @Test
    void testGetUnread_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(get("/api/notifications/unread"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user-123", roles = {"USER_1"})
    void testGetUnread_WithAuth_ReturnsNotifications() throws Exception {
        List<Notification> notifications = List.of(createTestNotification());
        when(notificationService.getUnreadForUser("user-123")).thenReturn(notifications);

        mockMvc.perform(get("/api/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("test-id"))
                .andExpect(jsonPath("$[0].title").value("Test Title"))
                .andExpect(jsonPath("$[0].message").value("Test Message"))
                .andExpect(jsonPath("$[0].read").value(false));

        verify(notificationService).getUnreadForUser("user-123");
    }

    @Test
    @WithMockUser(username = "user-123", roles = {"USER_1"})
    void testMarkAsRead_WithCorrectUser_MarksAsRead() throws Exception {
        mockMvc.perform(put("/api/notifications/123/read")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(notificationService).markAsReadForUser(eq(123L), eq("user-123"));
    }

    @Test
    @WithMockUser(username = "wrong-user", roles = {"USER_1"})
    void testMarkAsRead_WithWrongUser_Returns403() throws Exception {
        doThrow(new SecurityException("Access denied to notification"))
                .when(notificationService).markAsReadForUser(eq(123L), eq("wrong-user"));

        mockMvc.perform(put("/api/notifications/123/read")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(notificationService).markAsReadForUser(eq(123L), eq("wrong-user"));
    }

    @Test
    @WithMockUser(username = "user-123", roles = {"USER_1"})
    void testMarkAllAsRead_MarksAllAsRead() throws Exception {
        mockMvc.perform(put("/api/notifications/read-all")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(notificationService).markAllAsRead("user-123");
    }

    @Test
    @WithMockUser(username = "user-123", roles = {"USER_1"})
    void testCreateNotification_WithValidData_CreatesNotification() throws Exception {
        Notification notification = createTestNotification();
        when(notificationService.createAndSend(any(), any(), any(), any(), any(), any()))
                .thenReturn(notification);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "userId": "user-123",
                                "type": "info",
                                "title": "Test Title",
                                "message": "Test Message",
                                "relatedId": "related-123",
                                "relatedRoomSlug": "room-slug"
                            }
                            """)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-id"))
                .andExpect(jsonPath("$.title").value("Test Title"));

        verify(notificationService).createAndSend(
                eq("user-123"), eq("info"), eq("Test Title"), eq("Test Message"),
                eq("related-123"), eq("room-slug"));
    }

    @Test
    @WithMockUser(username = "user-123", roles = {"USER_1"})
    void testCreateNotification_WithInvalidData_Returns400() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "userId": "",
                                "type": "",
                                "title": ""
                            }
                            """)
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).createAndSend(any(), any(), any(), any(), any(), any());
    }
}