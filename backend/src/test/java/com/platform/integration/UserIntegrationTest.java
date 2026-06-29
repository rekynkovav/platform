package com.platform.integration;

import com.platform.model.User;
import com.platform.model.dto.UserDto;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for User endpoints.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserIntegrationTest extends AbstractIntegrationTest {

    @Autowired private UserRepository userRepository;

    private String testUserId;
    private String testUsername;
    private org.springframework.security.oauth2.jwt.Jwt testJwt;
    private org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken testAuth;

    @BeforeEach
    void setUp() {
        testUserId = userId();
        testUsername = "user-" + testUserId.substring(0, 8);
        testJwt = createJwt(testUserId, testUsername, "USER_0");
        testAuth = createAuth(testJwt, "USER_0");
    }

    @Test
    @Order(1)
    @DisplayName("Direct save + query — verify H2 persistence works")
    void directSaveAndQuery() throws Exception {
        User saved = userRepository.save(User.builder()
                .id(UUID.fromString(testUserId))
                .username(testUsername)
                .email("test@test.com")
                .role("USER_0")
                .version(0L)
                .build());

        // Verify directly via repository
        var found = userRepository.findById(UUID.fromString(testUserId));
        Assertions.assertTrue(found.isPresent(), "User must be found via repository after save");
        Assertions.assertEquals(testUsername, found.get().getUsername());
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/users/me - current user profile (auto-provision from JWT)")
    void getCurrentUser() throws Exception {
        // Не сохраняем вручную — syncUser создаст из JWT
        String expectedEmail = testUsername + "@test.com";

        mockMvc.perform(get("/api/users/me")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(testAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(testUsername))
                .andExpect(jsonPath("$.email").value(expectedEmail));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/users/me - auto-provisions new user from JWT")
    void getCurrentUserNotFound() throws Exception {
        String unknownId = userId();
        var unknownJwt = createJwt(unknownId, "ghost-" + unknownId.substring(0, 8), "USER_0");
        var unknownAuth = createAuth(unknownJwt, "USER_0");

        String expectedEmail = "ghost-" + unknownId.substring(0, 8) + "@test.com";

        mockMvc.perform(get("/api/users/me")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(unknownAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ghost-" + unknownId.substring(0, 8)))
                .andExpect(jsonPath("$.email").value(expectedEmail));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/users/{id} - public profile by ID")
    void getUserById() throws Exception {
        User saved = userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .username(testUsername)
                .role("USER_0")
                .bio("Hello world")
                .version(0L)
                .build());

        mockMvc.perform(get("/api/users/{userId}", saved.getId().toString())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(testAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(testUsername))
                .andExpect(jsonPath("$.bio").value("Hello world"));
    }

    @Test
    @Order(5)
    @DisplayName("PUT /api/users/me - update profile")
    void updateProfile() throws Exception {
        userRepository.save(User.builder()
                .id(UUID.fromString(testUserId))
                .username(testUsername)
                .email("old@test.com")
                .role("USER_0")
                .version(0L)
                .build());

        var updateRequest = new UserDto.UpdateRequest();
        updateRequest.setBio("Updated bio");

        mockMvc.perform(put("/api/users/me")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(testAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Updated bio"));
    }

    @Test
    @Order(6)
    @DisplayName("User lookup - create in DB then find")
    void userRegistrationFlow() throws Exception {
        String newUserId = userId();
        mockMvc.perform(get("/api/users/{userId}", newUserId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(testAuth)))
                .andExpect(status().isNotFound());

        userRepository.save(User.builder()
                .id(UUID.fromString(newUserId))
                .username("new-" + newUserId.substring(0, 8))
                .email("new@test.com")
                .role("USER_0")
                .version(0L)
                .build());

        mockMvc.perform(get("/api/users/{userId}", newUserId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(testAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new-" + newUserId.substring(0, 8)));
    }
}
