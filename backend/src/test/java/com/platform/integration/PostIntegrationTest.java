package com.platform.integration;

import com.platform.model.Post;
import com.platform.repository.PostRepository;
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
 * Integration tests for Post CRUD flow.
 * Full stack: Controller → Service → Repository → H2 (create-drop).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostIntegrationTest extends AbstractIntegrationTest {

    @Autowired private PostRepository postRepository;

    private String adminUserId;
    private org.springframework.security.oauth2.jwt.Jwt adminJwt;
    private org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken adminAuth;

    @BeforeEach
    void setUp() {
        adminUserId = userId();
        adminJwt = createJwt(adminUserId, "admin", "SUPER_ADMIN");
        adminAuth = createAuth(adminJwt, "SUPER_ADMIN");
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/feed/posts — create post as admin")
    void createPost() throws Exception {
        var request = new com.platform.model.dto.PostDto.CreateRequest();
        request.setContent("Test post content for integration test");
        request.setCategory("nutrition");

        mockMvc.perform(post("/api/feed/posts")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Test post content for integration test"))
                .andExpect(jsonPath("$.category").value("nutrition"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/feed/main — retrieve feed with created posts")
    void getFeed() throws Exception {
        postRepository.save(Post.builder()
                .content("Feed test post")
                .authorId(adminUserId)
                .authorName("admin")
                .category("general")
                .build());

        mockMvc.perform(get("/api/feed/main")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/feed/nutrition — category-filtered feed")
    void getNutritionFeed() throws Exception {
        postRepository.save(Post.builder()
                .content("Nutrition post 1")
                .authorId(adminUserId)
                .authorName("admin")
                .category("nutrition")
                .build());
        postRepository.save(Post.builder()
                .content("Nutrition post 2")
                .authorId(adminUserId)
                .authorName("admin")
                .category("nutrition")
                .build());
        postRepository.save(Post.builder()
                .content("Sport post")
                .authorId(adminUserId)
                .authorName("admin")
                .category("sport")
                .build());

        mockMvc.perform(get("/api/feed/nutrition")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.category != 'nutrition')]").doesNotExist());
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/posts/{id} — get single post by ID")
    void getPostById() throws Exception {
        Post post = postRepository.save(Post.builder()
                .content("Single post test")
                .authorId(adminUserId)
                .authorName("admin")
                .category("general")
                .build());

        mockMvc.perform(get("/api/posts/{id}", post.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(post.getId()))
                .andExpect(jsonPath("$.content").value("Single post test"));
    }

    @Test
    @Order(5)
    @DisplayName("PUT /api/feed/posts/{id} — update post")
    void updatePost() throws Exception {
        Post post = postRepository.save(Post.builder()
                .content("Original content")
                .authorId(adminUserId)
                .authorName("admin")
                .category("general")
                .build());

        var updateRequest = new com.platform.model.dto.PostDto.UpdateRequest();
        updateRequest.setContent("Updated content");

        mockMvc.perform(put("/api/feed/posts/{id}", post.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated content"));
    }

    @Test
    @Order(6)
    @DisplayName("DELETE /api/feed/posts/{id} — delete post (soft delete)")
    void deletePost() throws Exception {
        Post post = postRepository.save(Post.builder()
                .content("Post to delete")
                .authorId(adminUserId)
                .authorName("admin")
                .category("general")
                .build());

        mockMvc.perform(delete("/api/feed/posts/{id}", post.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/feed/posts — 403 for non-admin user")
    void createPostForbiddenForUser() throws Exception {
        Jwt userJwt = createJwt(userId(), "user", "USER_0");
        JwtAuthenticationToken userAuth = createAuth(userJwt, "USER_0");

        var request = new com.platform.model.dto.PostDto.CreateRequest();
        request.setContent("Should fail");
        request.setCategory("general");

        mockMvc.perform(post("/api/feed/posts")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(userAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/feed/main — pagination with 'before' cursor")
    void getFeedWithCursor() throws Exception {
        for (int i = 0; i < 5; i++) {
            postRepository.save(Post.builder()
                    .content("Paginated post " + i)
                    .authorId(adminUserId)
                    .authorName("admin")
                    .category("general")
                    .build());
        }

        var result = mockMvc.perform(get("/api/feed/main")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        var posts = objectMapper.readValue(response, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<com.platform.model.dto.PostDto>>() {});
        String lastCreatedAt = posts.get(posts.size() - 1).getCreatedAt().toString();

        mockMvc.perform(get("/api/feed/main")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .param("limit", "2")
                        .param("before", lastCreatedAt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
