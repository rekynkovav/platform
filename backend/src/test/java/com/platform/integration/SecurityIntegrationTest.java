package com.platform.integration;

import com.platform.model.User;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Security cross-cutting concerns.
 * Verifies 401/403 for different roles across major endpoints.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired private UserRepository userRepository;

    private Jwt regularJwt;
    private JwtAuthenticationToken regularAuth;
    private Jwt adminJwt;
    private JwtAuthenticationToken adminAuth;

    @BeforeEach
    void setUp() {
        String rid = userId();
        regularJwt = createJwt(rid, "sec-user-" + rid.substring(0, 8), "USER_0");
        regularAuth = createAuth(regularJwt, "USER_0");
        userRepository.save(User.builder().id(UUID.fromString(rid))
                .username("sec-user-" + rid.substring(0, 8)).role("USER_0").version(0L).build());

        String aid = userId();
        adminJwt = createJwt(aid, "sec-admin-" + aid.substring(0, 8), "SUPER_ADMIN");
        adminAuth = createAuth(adminJwt, "SUPER_ADMIN");
        userRepository.save(User.builder().id(UUID.fromString(aid))
                .username("sec-admin-" + aid.substring(0, 8)).role("SUPER_ADMIN").version(0L).build());
    }

    // --- Health endpoints: public ---

    @Test
    @DisplayName("GET /api/health — public, no auth required")
    void healthPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/events/upcoming — public, no auth required")
    void eventsUpcomingPublic() throws Exception {
        mockMvc.perform(get("/api/events/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // --- Admin endpoints: admin only ---

    @Test
    @DisplayName("GET /api/admin/stats — 403 for regular user")
    void adminStatsRegularUserForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(regularAuth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/stats — 200 for super admin")
    void adminStatsSuperAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk());
    }

    // --- Dashboard: admin only ---

    @Test
    @DisplayName("POST /api/dashboard/menu — 403 for regular user")
    void dashboardForbidden() throws Exception {
        mockMvc.perform(post("/api/dashboard/menu")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(regularAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // --- Broadcast creation: admin roles only ---

    @Test
    @DisplayName("POST /api/broadcast/create — 403 for regular user")
    void broadcastCreateForbidden() throws Exception {
        mockMvc.perform(post("/api/broadcast/create")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(regularAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"test\",\"streamType\":\"video\"}"))
                .andExpect(status().isForbidden());
    }

    // --- User profile: authenticated users ---

    @Test
    @DisplayName("GET /api/users/me — works for any authenticated user")
    void userProfileWorks() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(regularAuth)))
                .andExpect(status().isOk());
    }

    // --- Post creation: admin roles only ---

    @Test
    @DisplayName("POST /api/feed/posts — 403 for regular user")
    void createPostForbidden() throws Exception {
        mockMvc.perform(post("/api/feed/posts")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(regularAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"test\",\"category\":\"general\"}"))
                .andExpect(status().isForbidden());
    }

    // --- Archive creation: admin roles only ---

    @Test
    @DisplayName("POST /api/archives — 403 for regular user")
    void createArchiveForbidden() throws Exception {
        mockMvc.perform(post("/api/archives")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(regularAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    // --- Chat rooms: authenticated users ---

    @Test
    @DisplayName("GET /api/chat/rooms — works for regular user")
    void chatRoomsWork() throws Exception {
        mockMvc.perform(get("/api/chat/rooms")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(regularAuth)))
                .andExpect(status().isOk());
    }
}
