package com.platform.integration;

import com.platform.model.Comment;
import com.platform.model.Post;
import com.platform.model.User;
import com.platform.repository.CommentRepository;
import com.platform.repository.PostRepository;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Comment CRUD flow.
 * Full stack: Controller → Service → Repository → H2.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CommentIntegrationTest extends AbstractIntegrationTest {

    @Autowired private PostRepository postRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private UserRepository userRepository;

    private String adminUserId;
    private String adminUsername;
    private org.springframework.security.oauth2.jwt.Jwt adminJwt;
    private org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken adminAuth;

    @BeforeEach
    void setUp() {
        adminUserId = userId();
        adminUsername = "commenter-" + adminUserId.substring(0, 8);
        adminJwt = createJwt(adminUserId, adminUsername, "SUPER_ADMIN");
        adminAuth = createAuth(adminJwt, "SUPER_ADMIN");

        userRepository.save(User.builder()
                .id(UUID.fromString(adminUserId))
                .username(adminUsername)
                .email(adminUsername + "@test.com")
                .role("SUPER_ADMIN")
                .version(0L)
                .build());
    }

    private Post createTestPost(String content) {
        return postRepository.save(Post.builder()
                .content(content)
                .authorId(adminUserId)
                .authorName(adminUsername)
                .category("general")
                .likesCount(0)
                .commentsCount(0)
                .sharesCount(0)
                .viewsCount(0)
                .flagged(false)
                .build());
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/posts/{postId}/comments — create comment on post")
    void createComment() throws Exception {
        Post post = createTestPost("Post for commenting");

        var request = new com.platform.model.dto.CommentDto.CreateRequest();
        request.setContent("This is a test comment");

        mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("This is a test comment"))
                .andExpect(jsonPath("$.authorId").value(adminUserId))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/posts/{postId}/comments — list comments for post")
    void getComments() throws Exception {
        Post post = createTestPost("Post with comments");

        commentRepository.save(Comment.builder()
                .content("Comment 1")
                .postId(post.getId())
                .authorId(adminUserId)
                .authorName(adminUsername)
                .build());
        commentRepository.save(Comment.builder()
                .content("Comment 2")
                .postId(post.getId())
                .authorId(adminUserId)
                .authorName(adminUsername)
                .build());

        mockMvc.perform(get("/api/posts/{postId}/comments", post.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/posts/{postId}/comments — empty list for post with no comments")
    void getCommentsEmpty() throws Exception {
        Post post = createTestPost("Post with no comments");

        mockMvc.perform(get("/api/posts/{postId}/comments", post.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Order(4)
    @DisplayName("DELETE /api/posts/{postId}/comments/{commentId} — delete own comment")
    void deleteOwnComment() throws Exception {
        Post post = createTestPost("Post for deletion");

        Comment comment = commentRepository.save(Comment.builder()
                .content("Delete me")
                .postId(post.getId())
                .authorId(adminUserId)
                .authorName(adminUsername)
                .build());

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", post.getId(), comment.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/posts/{postId}/comments — 404 for non-existent post")
    void createCommentOnNonExistentPost() throws Exception {
        var request = new com.platform.model.dto.CommentDto.CreateRequest();
        request.setContent("Comment on non-existent");

        mockMvc.perform(post("/api/posts/{postId}/comments", UUID.randomUUID().toString())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(6)
    @DisplayName("Comments on multiple posts — isolation check")
    void commentsIsolation() throws Exception {
        Post post1 = createTestPost("Post 1");
        Post post2 = createTestPost("Post 2");

        commentRepository.save(Comment.builder()
                .content("Comment on post 1")
                .postId(post1.getId())
                .authorId(adminUserId)
                .authorName(adminUsername)
                .build());
        commentRepository.save(Comment.builder()
                .content("Comment on post 2")
                .postId(post2.getId())
                .authorId(adminUserId)
                .authorName(adminUsername)
                .build());

        mockMvc.perform(get("/api/posts/{postId}/comments", post1.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("Comment on post 1"));
    }
}
