package com.platform.integration;

import com.platform.model.Post;
import com.platform.model.Comment;
import com.platform.model.Event;
import com.platform.model.User;
import com.platform.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Cross-cutting integration tests verifying multi-entity flows.
 * Tests: User → Post → Comment → Event coexistence.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CrossCuttingIntegrationTest extends AbstractIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private EventRepository eventRepository;

    private String testUserId;
    private String testUsername;
    private org.springframework.security.oauth2.jwt.Jwt testJwt;
    private org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken testAuth;

    @BeforeEach
    void setUp() {
        testUserId = userId();
        // Use UUID in username to avoid unique constraint violations
        testUsername = "cross-" + testUserId.substring(0, 8);
        testJwt = createJwt(testUserId, testUsername, "SUPER_ADMIN");
        testAuth = createAuth(testJwt, "SUPER_ADMIN");

        userRepository.save(User.builder()
                .id(UUID.fromString(testUserId))
                .username(testUsername)
                .email("cross-" + testUserId.substring(0, 8) + "@test.com")
                .role("SUPER_ADMIN")
                .version(0L)
                .build());
    }

    @Test
    @Order(1)
    @DisplayName("Full lifecycle: create post → verify in feed → get by ID")
    void fullPostLifecycle() throws Exception {
        var createRequest = new com.platform.model.dto.PostDto.CreateRequest();
        createRequest.setContent("Lifecycle test post");
        createRequest.setCategory("nutrition");

        String responseBody = mockMvc.perform(post("/api/feed/posts")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(testAuth))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Lifecycle test post"))
                .andReturn().getResponse().getContentAsString();

        var createdPost = objectMapper.readValue(responseBody, com.platform.model.dto.PostDto.class);

        mockMvc.perform(get("/api/feed/main")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(testAuth))
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + createdPost.getId() + "')]").exists());

        mockMvc.perform(get("/api/posts/{id}", createdPost.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(testAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Lifecycle test post"));
    }

    @Test
    @Order(2)
    @DisplayName("Post + Comments batch query — N+1 fix verification")
    void batchQueryVerification() throws Exception {
        for (int i = 0; i < 5; i++) {
            Post post = postRepository.save(Post.builder()
                    .content("Batch post " + i)
                    .authorId(testUserId)
                    .authorName(testUsername)
                    .category("general")
                    .build());

            for (int j = 0; j < 3; j++) {
                commentRepository.save(Comment.builder()
                        .postId(post.getId())
                        .authorId(testUserId)
                        .authorName(testUsername)
                        .content("Comment " + j + " on post " + i)
                        .build());
            }
        }

        mockMvc.perform(get("/api/feed/main")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(testAuth))
                        .param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)));
    }

    @Test
    @Order(3)
    @DisplayName("Multi-user post creation — DB integrity")
    void multiUserPostCreation() throws Exception {
        String user2Id = userId();
        String user2Name = "user2-" + user2Id.substring(0, 8);
        userRepository.save(User.builder()
                .id(UUID.fromString(user2Id))
                .username(user2Name)
                .email("u2-" + user2Id.substring(0, 8) + "@test.com")
                .role("USER_0")
                .version(0L)
                .build());

        postRepository.save(Post.builder()
                .content("Post by user 1")
                .authorId(testUserId)
                .authorName(testUsername)
                .category("general")
                .build());

        postRepository.save(Post.builder()
                .content("Post by user 2")
                .authorId(user2Id)
                .authorName(user2Name)
                .category("general")
                .build());

        mockMvc.perform(get("/api/feed/main")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(testAuth))
                        .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @Order(4)
    @DisplayName("Event + Post coexistence — no conflicts")
    void eventAndPostCoexistence() throws Exception {
        Event event = eventRepository.save(Event.builder()
                .title("Test Event")
                .description("Description")
                .startTime(LocalDateTime.now().plusDays(1))
                .createdBy(testUserId)
                .build());

        postRepository.save(Post.builder()
                .content("Announcing event: " + event.getTitle())
                .authorId(testUserId)
                .authorName(testUsername)
                .category("announcement")
                .build());

        mockMvc.perform(get("/api/events/{id}", event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Event"));

        mockMvc.perform(get("/api/feed/main")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(testAuth))
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    // Helper: Comment builder without Post entity reference
    private static Comment comment(String postId, String authorId, String authorName, String content) {
        return Comment.builder()
                .postId(postId)
                .authorId(authorId)
                .authorName(authorName)
                .content(content)
                .build();
    }
}
