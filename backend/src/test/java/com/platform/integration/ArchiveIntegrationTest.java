package com.platform.integration;

import com.platform.model.Archive;
import com.platform.model.User;
import com.platform.repository.ArchiveRepository;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Archive CRUD flow.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ArchiveIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ArchiveRepository archiveRepository;
    @Autowired private UserRepository userRepository;

    private String adminUserId;
    private Jwt adminJwt;
    private JwtAuthenticationToken adminAuth;
    private String regularUserId;
    private Jwt regularJwt;
    private JwtAuthenticationToken regularAuth;

    @BeforeEach
    void setUp() {
        adminUserId = userId();
        adminJwt = createJwt(adminUserId, "arch-admin-" + adminUserId.substring(0, 8), "SUPER_ADMIN");
        adminAuth = createAuth(adminJwt, "SUPER_ADMIN");

        regularUserId = userId();
        regularJwt = createJwt(regularUserId, "arch-user-" + regularUserId.substring(0, 8), "USER_0");
        regularAuth = createAuth(regularJwt, "USER_0");

        userRepository.save(User.builder().id(UUID.fromString(adminUserId))
                .username("arch-admin-" + adminUserId.substring(0, 8)).role("SUPER_ADMIN").version(0L).build());
        userRepository.save(User.builder().id(UUID.fromString(regularUserId))
                .username("arch-user-" + regularUserId.substring(0, 8)).role("USER_0").version(0L).build());
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/archives — create archive as admin")
    void createArchive() throws Exception {
        var request = new com.platform.model.dto.ArchiveDto.CreateRequest();
        request.setTitle("Integration Test Archive");
        request.setDescription("Test description");
        request.setVideoUrl("https://example.com/video.mp4");
        request.setStartTime(LocalDateTime.now().minusDays(1));
        request.setEndTime(LocalDateTime.now());

        mockMvc.perform(post("/api/archives")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Integration Test Archive"));
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/archives — list archives")
    void listArchives() throws Exception {
        archiveRepository.save(Archive.builder()
                .id(UUID.randomUUID().toString()).title("Listed Archive")
                .videoUrl("https://example.com/v.mp4").viewsCount(0)
                .startTime(LocalDateTime.now().minusDays(1)).build());

        mockMvc.perform(get("/api/archives")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/archives/{id} — get archive by ID")
    void getArchiveById() throws Exception {
        Archive archive = archiveRepository.save(Archive.builder()
                .id(UUID.randomUUID().toString()).title("By ID Archive")
                .videoUrl("https://example.com/v2.mp4").viewsCount(0)
                .startTime(LocalDateTime.now().minusDays(1)).build());

        mockMvc.perform(get("/api/archives/{id}", archive.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("By ID Archive"));
    }

    @Test
    @Order(4)
    @DisplayName("PUT /api/archives/{id} — update archive as admin")
    void updateArchive() throws Exception {
        Archive archive = archiveRepository.save(Archive.builder()
                .id(UUID.randomUUID().toString()).title("Old Title")
                .videoUrl("https://example.com/v3.mp4").viewsCount(0)
                .startTime(LocalDateTime.now().minusDays(1)).build());

        var request = new com.platform.model.dto.ArchiveDto.UpdateRequest();
        request.setTitle("New Title");

        mockMvc.perform(put("/api/archives/{id}", archive.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));
    }

    @Test
    @Order(5)
    @DisplayName("DELETE /api/archives/{id} — delete archive as super admin")
    void deleteArchive() throws Exception {
        Archive archive = archiveRepository.save(Archive.builder()
                .id(UUID.randomUUID().toString()).title("Delete Me")
                .videoUrl("https://example.com/v4.mp4").viewsCount(0)
                .startTime(LocalDateTime.now().minusDays(60)).build());

        mockMvc.perform(delete("/api/archives/{id}", archive.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/archives — 403 for regular user")
    void createArchiveForbidden() throws Exception {
        var request = new com.platform.model.dto.ArchiveDto.CreateRequest();
        request.setTitle("Should fail");
        request.setStartTime(LocalDateTime.now());

        mockMvc.perform(post("/api/archives")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(regularAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    @DisplayName("PUT /api/archives/{id} — 403 for regular user")
    void updateArchiveForbidden() throws Exception {
        Archive archive = archiveRepository.save(Archive.builder()
                .id(UUID.randomUUID().toString()).title("Protected")
                .videoUrl("https://example.com/v5.mp4").viewsCount(0)
                .startTime(LocalDateTime.now().minusDays(1)).build());

        var request = new com.platform.model.dto.ArchiveDto.UpdateRequest();
        request.setTitle("Hacked");

        mockMvc.perform(put("/api/archives/{id}", archive.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(regularAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
