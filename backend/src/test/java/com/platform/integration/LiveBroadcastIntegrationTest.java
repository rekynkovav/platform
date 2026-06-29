package com.platform.integration;

import com.platform.model.BroadcastStatus;
import com.platform.model.LiveBroadcast;
import com.platform.model.User;
import com.platform.repository.LiveBroadcastRepository;
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
 * Integration tests for Live Broadcast endpoints.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LiveBroadcastIntegrationTest extends AbstractIntegrationTest {

    @Autowired private LiveBroadcastRepository broadcastRepository;
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
        adminJwt = createJwt(adminUserId, "bc-admin-" + adminUserId.substring(0, 8), "SUPER_ADMIN");
        adminAuth = createAuth(adminJwt, "SUPER_ADMIN");

        regularUserId = userId();
        regularJwt = createJwt(regularUserId, "bc-user-" + regularUserId.substring(0, 8), "USER_0");
        regularAuth = createAuth(regularJwt, "USER_0");

        userRepository.save(User.builder().id(UUID.fromString(adminUserId))
                .username("bc-admin-" + adminUserId.substring(0, 8)).role("SUPER_ADMIN").version(0L).build());
        userRepository.save(User.builder().id(UUID.fromString(regularUserId))
                .username("bc-user-" + regularUserId.substring(0, 8)).role("USER_0").version(0L).build());
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/broadcast/create — create broadcast as admin")
    void createBroadcast() throws Exception {
        mockMvc.perform(post("/api/broadcast/create")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test Stream\",\"description\":\"Integration test\",\"streamType\":\"video\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Stream"))
                .andExpect(jsonPath("$.status").value("IDLE"));
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/broadcast/active — list active broadcasts (needs auth)")
    void listActiveBroadcasts() throws Exception {
        broadcastRepository.save(LiveBroadcast.builder()
                .title("Active Stream").description("Live").streamType("video")
                .ownerId(UUID.fromString(adminUserId)).status(BroadcastStatus.LIVE)
                .build());

        mockMvc.perform(get("/api/broadcast/active")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/broadcast/{id} — get broadcast by ID")
    void getBroadcast() throws Exception {
        LiveBroadcast broadcast = broadcastRepository.save(LiveBroadcast.builder()
                .title("Get Me").description("Test").streamType("video")
                .ownerId(UUID.fromString(adminUserId)).status(BroadcastStatus.IDLE).build());

        mockMvc.perform(get("/api/broadcast/{id}", broadcast.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Get Me"));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/broadcast/{id}/stats — broadcast statistics")
    void getBroadcastStats() throws Exception {
        LiveBroadcast broadcast = broadcastRepository.save(LiveBroadcast.builder()
                .title("Stats Stream").description("Test").streamType("video")
                .ownerId(UUID.fromString(adminUserId)).status(BroadcastStatus.IDLE)
                .viewerCount(0).build());

        mockMvc.perform(get("/api/broadcast/{id}/stats", broadcast.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.broadcastId").value(broadcast.getId().toString()))
                .andExpect(jsonPath("$.viewerCount").isNumber());
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/broadcast/create — 403 for regular user")
    void createForbiddenForUser() throws Exception {
        mockMvc.perform(post("/api/broadcast/create")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(regularAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Should fail\",\"streamType\":\"video\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/broadcast/{id}/end — end broadcast as owner")
    void endBroadcast() throws Exception {
        LiveBroadcast broadcast = broadcastRepository.save(LiveBroadcast.builder()
                .title("End Me").description("Test").streamType("video")
                .ownerId(UUID.fromString(adminUserId)).status(BroadcastStatus.LIVE)
                .rtmpStreamKey(UUID.randomUUID().toString()).build());

        mockMvc.perform(post("/api/broadcast/{id}/end", broadcast.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(7)
    @DisplayName("DELETE /api/broadcast/{id} — delete broadcast as super admin")
    void deleteBroadcast() throws Exception {
        LiveBroadcast broadcast = broadcastRepository.save(LiveBroadcast.builder()
                .title("Delete Me").description("Test").streamType("video")
                .ownerId(UUID.fromString(adminUserId)).status(BroadcastStatus.IDLE).build());

        mockMvc.perform(delete("/api/broadcast/{id}", broadcast.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/broadcast/{id}/playlist.m3u8 — HLS playlist")
    void getHlsPlaylist() throws Exception {
        LiveBroadcast broadcast = broadcastRepository.save(LiveBroadcast.builder()
                .title("HLS Stream").description("Test").streamType("video")
                .ownerId(UUID.fromString(adminUserId)).status(BroadcastStatus.LIVE).build());

        mockMvc.perform(get("/api/broadcast/{id}/playlist.m3u8", broadcast.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.apple.mpegurl"));
    }
}
