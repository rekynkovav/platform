package com.platform.integration;

import com.platform.model.DashboardMenuItem;
import com.platform.model.User;
import com.platform.repository.DashboardMenuItemRepository;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Dashboard Menu CRUD.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DashboardMenuIntegrationTest extends AbstractIntegrationTest {

    @Autowired private DashboardMenuItemRepository menuRepository;
    @Autowired private UserRepository userRepository;

    private String adminUserId;
    private Jwt adminJwt;
    private JwtAuthenticationToken adminAuth;

    @BeforeEach
    void setUp() {
        adminUserId = userId();
        adminJwt = createJwt(adminUserId, "dash-" + adminUserId.substring(0, 8), "SUPER_ADMIN");
        adminAuth = createAuth(adminJwt, "SUPER_ADMIN");

        userRepository.save(User.builder().id(UUID.fromString(adminUserId))
                .username("dash-" + adminUserId.substring(0, 8)).role("SUPER_ADMIN").version(0L).build());
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/dashboard/menu — get menu data")
    void getMenu() throws Exception {
        mockMvc.perform(get("/api/dashboard/menu")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/dashboard/menu — menu with items")
    void getMenuWithData() throws Exception {
        menuRepository.save(DashboardMenuItem.builder()
                .sectionKey("test-section").title("Item 1").mediaType("text")
                .sortOrder(0).createdBy(UUID.fromString(adminUserId)).build());

        mockMvc.perform(get("/api/dashboard/menu")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['test-section']").isArray())
                .andExpect(jsonPath("$.['test-section'].length()").value(1))
                .andExpect(jsonPath("$.['test-section'][0].title").value("Item 1"));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/dashboard/menu — save menu data (full replace)")
    void saveMenuData() throws Exception {
        var menuData = Map.of(
                "broadcasts", List.of(
                        Map.of("title", "Stream 1", "mediaType", "video", "description", "Test stream")
                ),
                "exercises", List.of(
                        Map.of("title", "Exercise 1", "mediaType", "text"),
                        Map.of("title", "Exercise 2", "mediaType", "link")
                )
        );

        mockMvc.perform(post("/api/dashboard/menu")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menuData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true))
                .andExpect(jsonPath("$.sectionsCount").value(2))
                .andExpect(jsonPath("$.itemsCount").value(3));
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/dashboard/menu — 403 for regular user")
    void saveMenuForbidden() throws Exception {
        String regularUserId = userId();
        var regularJwt = createJwt(regularUserId, "dash-user-" + regularUserId.substring(0, 8), "USER_0");
        var regularAuth = createAuth(regularJwt, "USER_0");
        userRepository.save(User.builder().id(UUID.fromString(regularUserId))
                .username("dash-user-" + regularUserId.substring(0, 8)).role("USER_0").version(0L).build());

        mockMvc.perform(post("/api/dashboard/menu")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(regularAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"test\":[{\"title\":\"fail\"}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/dashboard/menu — full replace overwrites old data")
    void fullReplace() throws Exception {
        // Seed old data via API
        var oldData = Map.of("old-section", List.of(Map.of("title", "Old Item", "mediaType", "text")));
        mockMvc.perform(post("/api/dashboard/menu")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldData)))
                .andExpect(status().isOk());

        // Replace with new data
        var newData = Map.of("new-section", List.of(Map.of("title", "New Item", "mediaType", "video")));
        mockMvc.perform(post("/api/dashboard/menu")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true))
                .andExpect(jsonPath("$.itemsCount").value(1));

        // Verify new section present via repository
        long newCount = menuRepository.countBySectionKey("new-section");
        Assertions.assertEquals(1, newCount);
    }
}
