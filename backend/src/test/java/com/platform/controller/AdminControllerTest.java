package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.model.dto.AdminStatsDto;
import com.platform.model.dto.UserDto;
import com.platform.service.AdminService;
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
class AdminControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private String adminUserId;
    private UUID adminUserIdUuid;
    private String targetUserId;
    private UUID targetUserIdUuid;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
        objectMapper = new ObjectMapper();

        adminUserId = "admin-123";
        adminUserIdUuid = UUID.fromString(adminUserId);
        targetUserId = "target-456";
        targetUserIdUuid = UUID.fromString(targetUserId);
    }

    @Test
    void updateUserRole_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        String newRole = "MODERATOR_ADMIN";
        when(adminService.updateUserRole(eq(targetUserId), eq(newRole), eq(adminUserId)))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(put("/api/admin/users/{userId}/role", targetUserId)
                        .header("X-User-Id", adminUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"" + newRole + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateUserRole_InvalidRole_ReturnsBadRequest() throws Exception {
        // Arrange
        String invalidRole = "INVALID_ROLE";
        when(adminService.updateUserRole(eq(targetUserId), eq(invalidRole), eq(adminUserId)))
                .thenThrow(new IllegalArgumentException("Invalid role"));

        // Act & Assert
        mockMvc.perform(put("/api/admin/users/{userId}/role", targetUserId)
                        .header("X-User-Id", adminUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"" + invalidRole + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserRole_NotAuthorized_ReturnsForbidden() throws Exception {
        // Arrange
        String newRole = "MODERATOR_ADMIN";
        String regularUserId = "regular-789";
        when(adminService.updateUserRole(eq(targetUserId), eq(newRole), eq(regularUserId)))
                .thenThrow(new com.platform.exception.AccessDeniedException("Access denied"));

        // Act & Assert
        mockMvc.perform(put("/api/admin/users/{userId}/role", targetUserId)
                        .header("X-User-Id", regularUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"" + newRole + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void banUser_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        String banReason = "Violation of community guidelines";
        when(adminService.banUser(eq(targetUserId), eq(banReason), eq(adminUserId)))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/admin/users/{userId}/ban", targetUserId)
                        .header("X-User-Id", adminUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"" + banReason + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void banUser_EmptyReason_ReturnsBadRequest() throws Exception {
        // Arrange
        String emptyReason = "";

        // Act & Assert
        mockMvc.perform(post("/api/admin/users/{userId}/ban", targetUserId)
                        .header("X-User-Id", adminUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"" + emptyReason + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unbanUser_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        when(adminService.unbanUser(eq(targetUserId), eq(adminUserId)))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/admin/users/{userId}/unban", targetUserId)
                        .header("X-User-Id", adminUserId))
                .andExpect(status().isOk());
    }

    @Test
    void unbanUser_NotBanned_ReturnsBadRequest() throws Exception {
        // Arrange
        when(adminService.unbanUser(eq(targetUserId), eq(adminUserId)))
                .thenThrow(new IllegalStateException("User is not banned"));

        // Act & Assert
        mockMvc.perform(post("/api/admin/users/{userId}/unban", targetUserId)
                        .header("X-User-Id", adminUserId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void moderatePost_Approve_ReturnsOk() throws Exception {
        // Arrange
        String postId = "post-789";
        String moderationReason = "Compliant with guidelines";
        when(adminService.moderatePost(eq(postId), eq(true), eq(moderationReason), eq(adminUserId)))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/admin/posts/{postId}/moderate", postId)
                        .header("X-User-Id", adminUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\": true, \"reason\": \"" + moderationReason + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void moderatePost_Reject_ReturnsOk() throws Exception {
        // Arrange
        String postId = "post-789";
        String moderationReason = "Violates community guidelines";
        when(adminService.moderatePost(eq(postId), eq(false), eq(moderationReason), eq(adminUserId)))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/admin/posts/{postId}/moderate", postId)
                        .header("X-User-Id", adminUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\": false, \"reason\": \"" + moderationReason + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void moderatePost_EmptyReason_ReturnsBadRequest() throws Exception {
        // Arrange
        String postId = "post-789";
        String emptyReason = "";

        // Act & Assert
        mockMvc.perform(post("/api/admin/posts/{postId}/moderate", postId)
                        .header("X-User-Id", adminUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\": true, \"reason\": \"" + emptyReason + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStats_ValidRequest_ReturnsStats() throws Exception {
        // Arrange
        AdminStatsDto stats = AdminStatsDto.builder()
                .totalUsers(150L)
                .totalPosts(300L)
                .totalComments(450L)
                .totalEvents(60L)
                .activeUsers(75L)
                .build();

        when(adminService.getStats()).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/admin/stats")
                        .header("X-User-Id", adminUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(150))
                .andExpect(jsonPath("$.totalPosts").value(300))
                .andExpect(jsonPath("$.totalComments").value(450))
                .andExpect(jsonPath("$.totalEvents").value(60))
                .andExpect(jsonPath("$.activeUsers").value(75));
    }

    @Test
    void getStats_NotAuthorized_ReturnsForbidden() throws Exception {
        // Arrange
        String regularUserId = "regular-789";
        // This endpoint should check for admin role
        // For simplicity, we'll assume it returns 403 for non-admins

        // Act & Assert
        mockMvc.perform(get("/api/admin/stats")
                        .header("X-User-Id", regularUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUser_ValidRequest_ReturnsUserDto() throws Exception {
        // Arrange
        UserDto user = UserDto.builder()
                .id(targetUserIdUuid)
                .username("targetuser")
                .email("target@example.com")
                .role("USER_1")
                .banned(false)
                .createdAt(Instant.now().minusDays(30))
                .lastLogin(Instant.now().minusHours(2))
                .build();

        when(adminService.getUserById(eq(targetUserId))).thenReturn(user);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users/{userId}", targetUserId)
                        .header("X-User-Id", adminUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetUserId))
                .andExpect(jsonPath("$.username").value("targetuser"))
                .andExpect(jsonPath("$.email").value("target@example.com"))
                .andExpect(jsonPath("$.role").value("USER_1"))
                .andExpect(jsonPath("$.banned").value(false));
    }

    @Test
    void getUser_NotFound_ReturnsNotFound() throws Exception {
        // Arrange
        String nonExistentUserId = "non-existent-user";
        when(adminService.getUserById(eq(nonExistentUserId)))
                .thenThrow(new com.platform.exception.ResourceNotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(get("/api/admin/users/{userId}", nonExistentUserId)
                        .header("X-User-Id", adminUserId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUsers_ValidRequest_ReturnsUserList() throws Exception {
        // Arrange
        UserDto user1 = UserDto.builder()
                .id(UUID.randomUUID())
                .username("user1")
                .email("user1@example.com")
                .role("USER_1")
                .banned(false)
                .createdAt(Instant.now().minusDays(15))
                .build();

        UserDto user2 = UserDto.builder()
                .id(UUID.randomUUID())
                .username("user2")
                .email("user2@example.com")
                .role("MODERATOR")
                .banned(true)
                .banReason("Spamming")
                .bannedAt(Instant.now().minusDays(3))
                .createdAt(Instant.now().minusDays(20))
                .build();

        List<UserDto> users = List.of(user1, user2);

        when(adminService.getUsers(anyInt(), anyInt())).thenReturn(users);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                        .header("X-User-Id", adminUserId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("user1"))
                .andExpect(jsonPath("$[1].username").value("user2"));
    }

    @Test
    void getUsers_DefaultPagination_ReturnsUsers() throws Exception {
        // Arrange
        List<UserDto> users = List.of(
                UserDto.builder()
                        .id(UUID.randomUUID())
                        .username("user1")
                        .email("user1@example.com")
                        .role("USER_1")
                        .build()
        );

        when(adminService.getUsers(eq(0), eq(10))).thenReturn(users);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                        .header("X-User-Id", adminUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getFlaggedPosts_ValidRequest_ReturnsPosts() throws Exception {
        // Arrange
        // This would typically return a list of flagged posts
        // For simplicity, we'll return an empty list

        when(adminService.getFlaggedPosts(anyInt(), anyInt())).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/admin/posts/flagged")
                        .header("X-User-Id", adminUserId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getFlaggedPosts_NotAuthorized_ReturnsForbidden() throws Exception {
        // Arrange
        String regularUserId = "regular-789";

        // Act & Assert
        mockMvc.perform(get("/api/admin/posts/flagged")
                        .header("X-User-Id", regularUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    void getBannedUsers_ValidRequest_ReturnsUsers() throws Exception {
        // Arrange
        UserDto bannedUser = UserDto.builder()
                .id(UUID.randomUUID())
                .username("banneduser")
                .email("banned@example.com")
                .role("USER_1")
                .banned(true)
                .banReason("Harassment")
                .bannedAt(Instant.now().minusDays(5))
                .createdAt(Instant.now().minusDays(30))
                .build();

        List<UserDto> users = List.of(bannedUser);

        when(adminService.getBannedUsers(anyInt(), anyInt())).thenReturn(users);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users/banned")
                        .header("X-User-Id", adminUserId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].banned").value(true))
                .andExpect(jsonPath("$[0].banReason").value("Harassment"));
    }

    @Test
    void getBannedUsers_NotAuthorized_ReturnsForbidden() throws Exception {
        // Arrange
        String regularUserId = "regular-789";

        // Act & Assert
        mockMvc.perform(get("/api/admin/users/banned")
                        .header("X-User-Id", regularUserId))
                .andExpect(status().isForbidden());
    }
}