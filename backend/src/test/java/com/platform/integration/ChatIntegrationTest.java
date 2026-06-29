package com.platform.integration;

import com.platform.model.ChatRoom;
import com.platform.model.ChatRoomPermission;
import com.platform.model.User;
import com.platform.repository.ChatRoomRepository;
import com.platform.repository.ChatRoomPermissionRepository;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Chat endpoints.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private ChatRoomPermissionRepository chatRoomPermissionRepository;
    @Autowired private UserRepository userRepository;

    private ChatRoom testRoom;
    private String userId1;
    private Jwt jwt1;
    private JwtAuthenticationToken auth1;

    @BeforeEach
    void setUp() {
        userId1 = userId();
        jwt1 = createJwt(userId1, "chatuser-" + userId1.substring(0, 8), "USER_0");
        auth1 = createAuth(jwt1, "USER_0");

        userRepository.save(User.builder().id(UUID.fromString(userId1))
                .username("chatuser-" + userId1.substring(0, 8)).role("USER_0").version(0L).build());

        // Create test chat room with permissions
        testRoom = chatRoomRepository.save(ChatRoom.builder()
                .slug("test-room-" + UUID.randomUUID().toString().substring(0, 8))
                .type("general")
                .name("Test Room")
                .isActive(true)
                .build());

        // Role must match what RoleUtils.getUserRole() returns — "USER_0" without ROLE_ prefix
        chatRoomPermissionRepository.save(ChatRoomPermission.builder()
                .chatRoom(testRoom)
                .role("USER_0")
                .permissionLevel("read_write")
                .build());
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/chat/rooms — list question rooms")
    void listRooms() throws Exception {
        mockMvc.perform(get("/api/chat/rooms")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/chat/{roomSlug} — get messages from room (empty)")
    void getRoomMessages() throws Exception {
        mockMvc.perform(get("/api/chat/{roomSlug}", testRoom.getSlug())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/chat/{roomSlug} — 404 for non-existent room")
    void getRoomNotFound() throws Exception {
        mockMvc.perform(get("/api/chat/{roomSlug}", "nonexistent-room")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/chat/{roomSlug} — send message to room")
    void sendMessageToRoom() throws Exception {
        mockMvc.perform(post("/api/chat/{roomSlug}", testRoom.getSlug())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello room!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hello room!"));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/chat/{roomSlug}/messages — paginated messages")
    void getPaginatedMessages() throws Exception {
        mockMvc.perform(get("/api/chat/{roomSlug}/messages", testRoom.getSlug())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/chat/unread — unread message count")
    void getUnreadCount() throws Exception {
        mockMvc.perform(get("/api/chat/unread")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/chat/{roomSlug}/users — room user list (stub)")
    void getRoomUsers() throws Exception {
        mockMvc.perform(get("/api/chat/{roomSlug}/users", testRoom.getSlug())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(8)
    @DisplayName("Access denied for room without permissions")
    void accessDeniedForRoom() throws Exception {
        ChatRoom restrictedRoom = chatRoomRepository.save(ChatRoom.builder()
                .slug("restricted-" + UUID.randomUUID().toString().substring(0, 8))
                .type("admin").name("Restricted").isActive(true).build());

        // No permission set for this room — should fail with 403
        mockMvc.perform(get("/api/chat/{roomSlug}", restrictedRoom.getSlug())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isForbidden());
    }
}
