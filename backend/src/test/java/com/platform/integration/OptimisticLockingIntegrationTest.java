package com.platform.integration;

import com.platform.model.User;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for @Version optimistic locking.
 * H2 may not increment version identically to PostgreSQL, so we focus on
 * the controller-layer behavior and conflict detection.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OptimisticLockingIntegrationTest extends AbstractIntegrationTest {

    @Autowired private UserRepository userRepository;

    private String testUserId;
    private Jwt testJwt;
    private JwtAuthenticationToken testAuth;

    @BeforeEach
    void setUp() {
        testUserId = userId();
        testJwt = createJwt(testUserId, "opt-" + testUserId.substring(0, 8), "USER_0");
        testAuth = createAuth(testJwt, "USER_0");

        userRepository.save(User.builder()
                .id(UUID.fromString(testUserId))
                .username("opt-" + testUserId.substring(0, 8))
                .role("USER_0")
                .bio("original")
                .version(0L)
                .build());
    }

    @Test
    @Order(1)
    @DisplayName("Successful update via controller — profile changes persist")
    void successfulUpdate() throws Exception {
        var updateRequest = new com.platform.model.dto.UserDto.UpdateRequest();
        updateRequest.setBio("updated bio");

        mockMvc.perform(put("/api/users/me")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(testAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("updated bio"));
    }

    @Test
    @Order(2)
    @DisplayName("Concurrent updates — second write with stale version fails")
    void concurrentUpdateConflict() {
        // Read current user — version 0
        User user1 = userRepository.findById(UUID.fromString(testUserId)).orElseThrow();
        User user2 = userRepository.findById(UUID.fromString(testUserId)).orElseThrow();

        assertEquals(0L, user1.getVersion());
        assertEquals(0L, user2.getVersion());

        // First update succeeds
        user1.setBio("update 1");
        userRepository.save(user1);

        // Second update with stale version should throw optimistic lock failure
        user2.setBio("update 2");
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            userRepository.save(user2);
        });
    }

    @Test
    @Order(3)
    @DisplayName("Sequential updates via controller — both succeed")
    void sequentialUpdatesSucceed() throws Exception {
        var req1 = new com.platform.model.dto.UserDto.UpdateRequest();
        req1.setBio("first update");
        mockMvc.perform(put("/api/users/me")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(testAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("first update"));

        var req2 = new com.platform.model.dto.UserDto.UpdateRequest();
        req2.setBio("second update");
        mockMvc.perform(put("/api/users/me")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(testAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("second update"));
    }

    @Test
    @Order(4)
    @DisplayName("User entity has @Version field — verified by schema")
    void versionFieldExists() {
        User user = userRepository.findById(UUID.fromString(testUserId)).orElseThrow();
        assertNotNull(user.getVersion(), "@Version field must be present");
    }
}
