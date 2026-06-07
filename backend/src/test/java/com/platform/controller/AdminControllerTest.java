package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.exception.GlobalExceptionHandler;
import com.platform.model.Post;
import com.platform.model.dto.AdminStatsDto;
import com.platform.model.dto.UserDto;
import com.platform.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
class AdminControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private AdminService adminService;
    @InjectMocks private AdminController adminController;

    private String userId;
    private Jwt adminJwt;
    private Jwt superAdminJwt;
    private JwtAuthenticationToken adminAuth;
    private JwtAuthenticationToken superAdminAuth;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .addFilters(securityContextFilter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        userId = UUID.randomUUID().toString();

        adminJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", UUID.randomUUID().toString())
                .claim("preferred_username", "moderator")
                .claim("realm_access.roles", "MODERATOR_ADMIN")
                .build();

        superAdminJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", UUID.randomUUID().toString())
                .claim("preferred_username", "superadmin")
                .claim("realm_access.roles", "SUPER_ADMIN")
                .build();

        adminAuth = new JwtAuthenticationToken(adminJwt, List.of(new SimpleGrantedAuthority("ROLE_MODERATOR_ADMIN")));
        superAdminAuth = new JwtAuthenticationToken(superAdminJwt, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));

        userDto = UserDto.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .role("USER_0")
                .banned(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== GET /api/admin/users ====================

    @Test
    void getAllUsers_AdminCanAccess_ReturnsPage() throws Exception {
        Page<UserDto> page = new PageImpl<>(List.of(userDto), PageRequest.of(0, 50), 1);
        when(adminService.getAllUsers(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "50")
                        .with(withAuth(superAdminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("testuser"));
    }

    // ==================== GET /api/admin/users/{userId} ====================

    @Test
    void getUser_Exists_ReturnsOk() throws Exception {
        when(adminService.getUserById(userId)).thenReturn(userDto);

        mockMvc.perform(get("/api/admin/users/{userId}", userId).with(withAuth(superAdminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId));
    }

    // ==================== PUT /api/admin/users/{userId}/role ====================

    @Test
    void updateUserRole_SuperAdminCanUpdate() throws Exception {
        UserDto updated = UserDto.builder()
                .id(userId).username("testuser").role("MODERATOR")
                .createdAt(LocalDateTime.now()).build();
        when(adminService.updateUserRole(eq(userId), eq("MODERATOR"), any(Jwt.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/admin/users/{userId}/role", userId)
                        .param("role", "MODERATOR")
                        .with(withAuth(superAdminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MODERATOR"));
    }

    // ==================== POST /api/admin/users/{userId}/ban ====================

    @Test
    void banUser_AdminCanBan_Returns204() throws Exception {
        doNothing().when(adminService).banUser(eq(userId), eq("Spam"), any(Jwt.class));

        mockMvc.perform(post("/api/admin/users/{userId}/ban", userId)
                        .param("reason", "Spam")
                        .with(withAuth(adminAuth)))
                .andExpect(status().isNoContent());
    }

    // ==================== POST /api/admin/users/{userId}/unban ====================

    @Test
    void unbanUser_AdminCanUnban_Returns204() throws Exception {
        doNothing().when(adminService).unbanUser(eq(userId), any(Jwt.class));

        mockMvc.perform(post("/api/admin/users/{userId}/unban", userId)
                        .with(withAuth(adminAuth)))
                .andExpect(status().isNoContent());
    }

    // ==================== GET /api/admin/posts/flagged ====================

    @Test
    void getFlaggedPosts_ReturnsList() throws Exception {
        Post flagged = Post.builder().id("post-1").content("Flagged content").flagged(true).build();
        when(adminService.getFlaggedPosts()).thenReturn(List.of(flagged));

        mockMvc.perform(get("/api/admin/posts/flagged").with(withAuth(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ==================== POST /api/admin/posts/{postId}/moderate ====================

    @Test
    void moderatePost_Approve_Returns204() throws Exception {
        doNothing().when(adminService).moderatePost(eq("post-1"), eq(true), isNull(), any(Jwt.class));

        mockMvc.perform(post("/api/admin/posts/{postId}/moderate", "post-1")
                        .param("approve", "true")
                        .with(withAuth(adminAuth)))
                .andExpect(status().isNoContent());
    }

    @Test
    void moderatePost_RejectWithReason_Returns204() throws Exception {
        doNothing().when(adminService).moderatePost(eq("post-1"), eq(false), eq("Inappropriate"), any(Jwt.class));

        mockMvc.perform(post("/api/admin/posts/{postId}/moderate", "post-1")
                        .param("approve", "false")
                        .param("reason", "Inappropriate")
                        .with(withAuth(adminAuth)))
                .andExpect(status().isNoContent());
    }

    // ==================== GET /api/admin/stats ====================

    @Test
    void getStats_ReturnsAggregatedStats() throws Exception {
        AdminStatsDto stats = AdminStatsDto.builder()
                .totalUsers(100L)
                .totalPosts(500L)
                .totalComments(2000L)
                .totalEvents(50L)
                .activeUsers(30L)
                .build();
        when(adminService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/stats").with(withAuth(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(100))
                .andExpect(jsonPath("$.totalPosts").value(500))
                .andExpect(jsonPath("$.activeUsers").value(30));
    }
}
