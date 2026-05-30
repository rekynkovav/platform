package com.platform.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.model.dto.BanUserDto;
import com.platform.model.dto.UpdateUserRoleDto;
import com.platform.model.entity.User;
import com.platform.model.enums.UserRole;
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
class AdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User regularUser1;
    private User regularUser2;
    private User bannedUser;
    private String adminId;
    private String regularUserId1;
    private String regularUserId2;
    private String bannedUserId;

    @BeforeEach
    void setUp() {
        // Create admin user
        adminUser = User.builder()
                .id(UUID.randomUUID())
                .username("admin_user")
                .email("admin@example.com")
                .passwordHash("hashed_password_admin")
                .role(UserRole.SUPER_ADMIN)
                .createdAt(Instant.now())
                .build();

        // Create regular users
        regularUser1 = User.builder()
                .id(UUID.randomUUID())
                .username("regular_user1")
                .email("regular1@example.com")
                .passwordHash("hashed_password_1")
                .role(UserRole.USER_1)
                .createdAt(Instant.now())
                .build();

        regularUser2 = User.builder()
                .id(UUID.randomUUID())
                .username("regular_user2")
                .email("regular2@example.com")
                .passwordHash("hashed_password_2")
                .role(UserRole.USER_1)
                .createdAt(Instant.now())
                .build();

        // Create banned user
        bannedUser = User.builder()
                .id(UUID.randomUUID())
                .username("banned_user")
                .email("banned@example.com")
                .passwordHash("hashed_password_banned")
                .role(UserRole.USER_1)
                .banned(true)
                .banReason("Violation of community guidelines")
                .bannedAt(Instant.now().minusDays(1))
                .createdAt(Instant.now().minusDays(10))
                .build();

        userRepository.save(adminUser);
        userRepository.save(regularUser1);
        userRepository.save(regularUser2);
        userRepository.save(bannedUser);

        adminId = adminUser.getId().toString();
        regularUserId1 = regularUser1.getId().toString();
        regularUserId2 = regularUser2.getId().toString();
        bannedUserId = bannedUser.getId().toString();
    }

    @Test
    void getAllUsers_AdminRequest_ReturnsUsers() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                        .header("X-User-Id", adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4)) // All 4 users
                .andExpect(jsonPath("$[0].username").exists())
                .andExpect(jsonPath("$[0].email").exists())
                .andExpect(jsonPath("$[0].role").exists())
                .andExpect(jsonPath("$[0].banned").exists());
    }

    @Test
    void getAllUsers_WithPagination_ReturnsPage() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                        .header("X-User-Id", adminId)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").exists())
                .andExpect(jsonPath("$[1].username").exists());
    }

    @Test
    void getAllUsers_NonAdmin_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                        .header("X-User-Id", regularUserId1))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_AdminRequest_ReturnsUser() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/users/{id}", regularUserId1)
                        .header("X-User-Id", adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(regularUserId1))
                .andExpect(jsonPath("$.username").value("regular_user1"))
                .andExpect(jsonPath("$.email").value("regular1@example.com"))
                .andExpect(jsonPath("$.role").value("USER_1"))
                .andExpect(jsonPath("$.banned").value(false));
    }

    @Test
    void getUserById_NonExistentUser_ReturnsNotFound() throws Exception {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();

        // Act & Assert
        mockMvc.perform(get("/api/admin/users/{id}", nonExistentId)
                        .header("X-User-Id", adminId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserById_NonAdmin_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/users/{id}", regularUserId2)
                        .header("X-User-Id", regularUserId1))
                .andExpect(status().isForbidden());
    }

    @Test
    void banUser_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        String banReason = "Spamming in chat";
        BanUserDto request = BanUserDto.builder()
                .reason(banReason)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/admin/users/{id}/ban", regularUserId1)
                        .header("X-User-Id", adminId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify user is banned
        mockMvc.perform(get("/api/admin/users/{id}", regularUserId1)
                        .header("X-User-Id", adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banned").value(true))
                .andExpect(jsonPath("$.banReason").value(banReason))
                .andExpect(jsonPath("$.bannedAt").exists());
    }

    @Test
    void banUser_AlreadyBanned_ReturnsBadRequest() throws Exception {
        // Arrange
        BanUserDto request = BanUserDto.builder()
                .reason("Additional violation")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/admin/users/{id}/ban", bannedUserId)
                        .header("X-User-Id", adminId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void banUser_NoReason_ReturnsBadRequest() throws Exception {
        // Arrange
        BanUserDto request = BanUserDto.builder()
                .reason("") // Empty reason
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/admin/users/{id}/ban", regularUserId1)
                        .header("X-User-Id", adminId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void banUser_NonAdmin_ReturnsForbidden() throws Exception {
        // Arrange
        BanUserDto request = BanUserDto.builder()
                .reason("Test reason")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/admin/users/{id}/ban", regularUserId2)
                        .header("X-User-Id", regularUserId1))
                .andExpect(status().isForbidden());
    }

    @Test
    void unbanUser_ValidRequest_ReturnsOk() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/admin/users/{id}/unban", bannedUserId)
                        .header("X-User-Id", adminId))
                .andExpect(status().isOk());

        // Verify user is unbanned
        mockMvc.perform(get("/api/admin/users/{id}", bannedUserId)
                        .header("X-User-Id", adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banned").value(false))
                .andExpect(jsonPath("$.banReason").doesNotExist())
                .andExpect(jsonPath("$.bannedAt").doesNotExist());
    }

    @Test
    void unbanUser_NotBanned_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/admin/users/{id}/unban", regularUserId1)
                        .header("X-User-Id", adminId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unbanUser_NonAdmin_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/admin/users/{id}/unban", bannedUserId)
                        .header("X-User-Id", regularUserId1))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUserRole_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        UpdateUserRoleDto request = UpdateUserRoleDto.builder()
                .role(UserRole.MODERATOR)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/admin/users/{id}/role", regularUserId1)
                        .header("X-User-Id", adminId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify role is updated
        mockMvc.perform(get("/api/admin/users/{id}", regularUserId1)
                        .header("X-User-Id", adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MODERATOR"));
    }

    @Test
    void updateUserRole_InvalidRole_ReturnsBadRequest() throws Exception {
        // Arrange
        // Create request with invalid role (using string since enum validation will fail)
        String invalidRequest = "{\"role\": \"INVALID_ROLE\"}";

        // Act & Assert
        mockMvc.perform(put("/api/admin/users/{id}/role", regularUserId1)
                        .header("X-User-Id", adminId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserRole_NonAdmin_ReturnsForbidden() throws Exception {
        // Arrange
        UpdateUserRoleDto request = UpdateUserRoleDto.builder()
                .role(UserRole.MODERATOR)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/admin/users/{id}/role", regularUserId2)
                        .header("X-User-Id", regularUserId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getBannedUsers_AdminRequest_ReturnsBannedUsers() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/users/banned")
                        .header("X-User-Id", adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)) // Only bannedUser
                .andExpect(jsonPath("$[0].id").value(bannedUserId))
                .andExpect(jsonPath("$[0].username").value("banned_user"))
                .andExpect(jsonPath("$[0].banned").value(true))
                .andExpect(jsonPath("$[0].banReason").value("Violation of community guidelines"));
    }

    @Test
    void getBannedUsers_WithPagination_ReturnsPage() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/users/banned")
                        .header("X-User-Id", adminId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getBannedUsers_NonAdmin_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/users/banned")
                        .header("X-User-Id", regularUserId1))
                .andExpect(status().isForbidden());
    }

    @Test
    void getActiveUsers_AdminRequest_ReturnsActiveUsers() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/users/active")
                        .header("X-User-Id", adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3)) // adminUser, regularUser1, regularUser2
                .andExpect(jsonPath("$[0].banned").value(false))
                .andExpect(jsonPath("$[1].banned").value(false))
                .andExpect(jsonPath("$[2].banned").value(false));
    }

    @Test
    void getActiveUsers_NonAdmin_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/users/active")
                        .header("X-User-Id", regularUserId1))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserStats_AdminRequest_ReturnsStats() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/stats/users")
                        .header("X-User-Id", adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(4))
                .andExpect(jsonPath("$.bannedUsers").value(1))
                .andExpect(jsonPath("$.activeUsers").value(3))
                .andExpect(jsonPath("$.usersByRole").exists());
    }

    @Test
    void getUserStats_NonAdmin_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/stats/users")
                        .header("X-User-Id", regularUserId1))
                .andExpect(status().isForbidden());
    }

    @Test
    void searchUsers_AdminRequest_ReturnsMatchingUsers() throws Exception {
        // Act & Assert - Search by username
        mockMvc.perform(get("/api/admin/users/search")
                        .header("X-User-Id", adminId)
                        .param("query", "regular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)) // regular_user1 and regular_user2
                .andExpect(jsonPath("$[0].username").value("regular_user1"))
                .andExpect(jsonPath("$[1].username").value("regular_user2"));
    }

    @Test
    void searchUsers_ByEmail_ReturnsMatchingUsers() throws Exception {
        // Act & Assert - Search by email
        mockMvc.perform(get("/api/admin/users/search")
                        .header("X-User-Id", adminId)
                        .param("query", "regular1@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("regular1@example.com"));
    }

    @Test
    void searchUsers_NoMatches_ReturnsEmptyList() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/users/search")
                        .header("X-User-Id", adminId)
                        .param("query", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void searchUsers_NonAdmin_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/users/search")
                        .header("X-User-Id", regularUserId1)
                        .param("query", "test"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_AdminRequest_ReturnsOk() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/admin/users/{id}", regularUserId1)
                        .header("X-User-Id", adminId))
                .andExpect(status().isOk());

        // Verify user is deleted
        mockMvc.perform(get("/api/admin/users/{id}", regularUserId1)
                        .header("X-User-Id", adminId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_NonExistentUser_ReturnsNotFound() throws Exception {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();

        // Act & Assert
        mockMvc.perform(delete("/api/admin/users/{id}", nonExistentId)
                        .header("X-User-Id", adminId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_NonAdmin_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/admin/users/{id}", regularUserId2)
                        .header("X-User-Id", regularUserId1))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSystemStats_AdminRequest_ReturnsStats() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/stats/system")
                        .header("X-User-Id", adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uptime").exists())
                .andExpect(jsonPath("$.memoryUsage").exists())
                .andExpect(jsonPath("$.activeConnections").exists())
                .andExpect(jsonPath("$.databaseStats").exists());
    }

    @Test
    void getSystemStats_NonAdmin_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/stats/system")
                        .header("X-User-Id", regularUserId1))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAuditLogs_AdminRequest_ReturnsLogs() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("X-User-Id", adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").exists());
    }

    @Test
    void getAuditLogs_WithPagination_ReturnsPage() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("X-User-Id", adminId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").exists());
    }

    @Test
    void getAuditLogs_NonAdmin_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("X-User-Id", regularUserId1))
                .andExpect(status().isForbidden());
    }
}