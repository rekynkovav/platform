package com.platform.integration;

import com.platform.model.EventMaterial;
import com.platform.model.User;
import com.platform.repository.EventMaterialRepository;
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
 * Integration tests for EventMaterial CRUD.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EventMaterialIntegrationTest extends AbstractIntegrationTest {

    @Autowired private EventMaterialRepository eventMaterialRepository;
    @Autowired private UserRepository userRepository;

    private String adminUserId;
    private Jwt adminJwt;
    private JwtAuthenticationToken adminAuth;
    private String sectionId;

    @BeforeEach
    void setUp() {
        adminUserId = userId();
        adminJwt = createJwt(adminUserId, "em-admin-" + adminUserId.substring(0, 8), "SUPER_ADMIN");
        adminAuth = createAuth(adminJwt, "SUPER_ADMIN");
        sectionId = "section-" + UUID.randomUUID().toString().substring(0, 8);

        userRepository.save(User.builder().id(UUID.fromString(adminUserId))
                .username("em-admin-" + adminUserId.substring(0, 8)).role("SUPER_ADMIN").version(0L).build());
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/events/materials — create material")
    void createMaterial() throws Exception {
        var request = new com.platform.model.dto.EventMaterialDto.CreateRequest();
        request.setSectionId(sectionId);
        request.setType("text");
        request.setTitle("Test Material");
        request.setContent("Material content");

        mockMvc.perform(post("/api/events/materials")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Material"))
                .andExpect(jsonPath("$.sectionId").value(sectionId));
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/events/materials/{sectionId} — list materials by section")
    void getMaterialsBySection() throws Exception {
        eventMaterialRepository.save(EventMaterial.builder()
                .sectionId(sectionId).type("text").title("Material 1")
                .content("Content 1").createdBy(adminUserId).authorName("admin")
                .sortOrder(0).isActive(true).build());
        eventMaterialRepository.save(EventMaterial.builder()
                .sectionId(sectionId).type("video").title("Material 2")
                .url("https://example.com/video.mp4").createdBy(adminUserId).authorName("admin")
                .sortOrder(1).isActive(true).build());

        mockMvc.perform(get("/api/events/materials/{sectionId}", sectionId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Material 1"));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/events/materials/item/{id} — get material by ID")
    void getMaterialById() throws Exception {
        EventMaterial material = eventMaterialRepository.save(EventMaterial.builder()
                .sectionId(sectionId).type("link").title("Link Material")
                .url("https://example.com").createdBy(adminUserId).authorName("admin")
                .sortOrder(0).isActive(true).build());

        mockMvc.perform(get("/api/events/materials/item/{id}", material.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Link Material"));
    }

    @Test
    @Order(4)
    @DisplayName("PUT /api/events/materials/{id} — update material")
    void updateMaterial() throws Exception {
        EventMaterial material = eventMaterialRepository.save(EventMaterial.builder()
                .sectionId(sectionId).type("text").title("Old Title")
                .content("Old content").createdBy(adminUserId).authorName("admin")
                .sortOrder(0).isActive(true).build());

        var request = new com.platform.model.dto.EventMaterialDto.UpdateRequest();
        request.setTitle("New Title");

        mockMvc.perform(put("/api/events/materials/{id}", material.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));
    }

    @Test
    @Order(5)
    @DisplayName("DELETE /api/events/materials/{id} — delete material")
    void deleteMaterial() throws Exception {
        EventMaterial material = eventMaterialRepository.save(EventMaterial.builder()
                .sectionId(sectionId).type("text").title("Delete Me")
                .createdBy(adminUserId).authorName("admin").sortOrder(0).isActive(true).build());

        mockMvc.perform(delete("/api/events/materials/{id}", material.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/events/materials — 403 for regular user")
    void createForbidden() throws Exception {
        String regularUserId = userId();
        var regularJwt = createJwt(regularUserId, "em-user-" + regularUserId.substring(0, 8), "USER_0");
        var regularAuth = createAuth(regularJwt, "USER_0");
        userRepository.save(User.builder().id(UUID.fromString(regularUserId))
                .username("em-user-" + regularUserId.substring(0, 8)).role("USER_0").version(0L).build());

        var request = new com.platform.model.dto.EventMaterialDto.CreateRequest();
        request.setSectionId(sectionId);
        request.setType("text");
        request.setTitle("Should fail");

        mockMvc.perform(post("/api/events/materials")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(regularAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
