package com.platform.integration;

import com.platform.model.Post;
import com.platform.model.User;
import com.platform.repository.PostRepository;
import com.platform.repository.ReactionRepository;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Reaction toggle on posts.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReactionIntegrationTest extends AbstractIntegrationTest {

    @Autowired private PostRepository postRepository;
    @Autowired private ReactionRepository reactionRepository;
    @Autowired private UserRepository userRepository;

    private String userId1;
    private String userId2;
    private Jwt jwt1;
    private Jwt jwt2;
    private JwtAuthenticationToken auth1;
    private JwtAuthenticationToken auth2;

    @BeforeEach
    void setUp() {
        userId1 = userId();
        jwt1 = createJwt(userId1, "reactor1-" + userId1.substring(0, 8), "USER_0");
        auth1 = createAuth(jwt1, "USER_0");

        userId2 = userId();
        jwt2 = createJwt(userId2, "reactor2-" + userId2.substring(0, 8), "USER_0");
        auth2 = createAuth(jwt2, "USER_0");

        userRepository.save(User.builder().id(UUID.fromString(userId1))
                .username("reactor1-" + userId1.substring(0, 8)).role("USER_0").version(0L).build());
        userRepository.save(User.builder().id(UUID.fromString(userId2))
                .username("reactor2-" + userId2.substring(0, 8)).role("USER_0").version(0L).build());
    }

    private Post createPost() {
        return postRepository.save(Post.builder()
                .content("Post for reactions").authorId(userId1).authorName("reactor1")
                .category("general").likesCount(0).commentsCount(0)
                .sharesCount(0).viewsCount(0).flagged(false).build());
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/posts/{postId}/reactions — add like reaction")
    void addReaction() throws Exception {
        Post post = createPost();

        mockMvc.perform(post("/api/posts/{postId}/reactions", post.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1))
                        .param("type", "like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userReacted").value(true))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/posts/{postId}/reactions — toggle removes existing reaction")
    void toggleReactionRemoves() throws Exception {
        Post post = createPost();

        // Add reaction
        mockMvc.perform(post("/api/posts/{postId}/reactions", post.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1))
                        .param("type", "like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userReacted").value(true));

        // Toggle off
        mockMvc.perform(post("/api/posts/{postId}/reactions", post.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1))
                        .param("type", "like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userReacted").value(false))
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/posts/{postId}/reactions — check reaction status")
    void getReactionStatus() throws Exception {
        Post post = createPost();

        mockMvc.perform(get("/api/posts/{postId}/reactions", post.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userReacted").value(false));
    }

    @Test
    @Order(4)
    @DisplayName("Multiple users can react to same post")
    void multipleUsersReact() throws Exception {
        Post post = createPost();

        mockMvc.perform(post("/api/posts/{postId}/reactions", post.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1))
                        .param("type", "like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(post("/api/posts/{postId}/reactions", post.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth2))
                        .param("type", "like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    @Order(5)
    @DisplayName("Different reaction types on same post")
    void differentReactionTypes() throws Exception {
        Post post = createPost();

        mockMvc.perform(post("/api/posts/{postId}/reactions", post.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1))
                        .param("type", "like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userReacted").value(true));

        mockMvc.perform(post("/api/posts/{postId}/reactions", post.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth1))
                        .param("type", "love"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userReacted").value(true));
    }
}
