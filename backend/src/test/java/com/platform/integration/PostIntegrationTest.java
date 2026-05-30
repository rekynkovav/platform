package com.platform.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.model.dto.PostDto;
import com.platform.model.entity.User;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;
    private String user1Id;
    private String user2Id;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser1 = User.builder()
                .id(UUID.randomUUID())
                .username("post_user1")
                .email("post1@example.com")
                .passwordHash("hashed_password_1")
                .createdAt(Instant.now())
                .build();

        testUser2 = User.builder()
                .id(UUID.randomUUID())
                .username("post_user2")
                .email("post2@example.com")
                .passwordHash("hashed_password_2")
                .createdAt(Instant.now())
                .build();

        userRepository.save(testUser1);
        userRepository.save(testUser2);

        user1Id = testUser1.getId().toString();
        user2Id = testUser2.getId().toString();
    }

    @Test
    void createPost_ValidRequest_ReturnsPostDto() throws Exception {
        // Arrange
        String postContent = "This is my first post on the platform!";
        PostDto request = PostDto.builder()
                .content(postContent)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/posts")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(postContent))
                .andExpect(jsonPath("$.authorId").value(user1Id))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.likesCount").value(0))
                .andExpect(jsonPath("$.commentsCount").value(0))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createPost_WithMedia_ReturnsPostDto() throws Exception {
        // Arrange
        String postContent = "Post with media attachment";
        String mediaUrl = "https://example.com/image.jpg";
        PostDto request = PostDto.builder()
                .content(postContent)
                .mediaUrl(mediaUrl)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/posts")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(postContent))
                .andExpect(jsonPath("$.mediaUrl").value(mediaUrl))
                .andExpect(jsonPath("$.authorId").value(user1Id));
    }

    @Test
    void createPost_EmptyContent_ReturnsBadRequest() throws Exception {
        // Arrange
        PostDto request = PostDto.builder()
                .content("")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/posts")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPost_NoUserId_ReturnsBadRequest() throws Exception {
        // Arrange
        PostDto request = PostDto.builder()
                .content("Test post")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPost_ValidRequest_ReturnsPostDto() throws Exception {
        // Arrange
        // First create a post
        PostDto createRequest = PostDto.builder()
                .content("Post to retrieve")
                .build();

        String response = mockMvc.perform(post("/api/posts")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PostDto createdPost = objectMapper.readValue(response, PostDto.class);
        String postId = createdPost.getId().toString();

        // Act & Assert - Get the post
        mockMvc.perform(get("/api/posts/{id}", postId)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.content").value("Post to retrieve"))
                .andExpect(jsonPath("$.authorId").value(user1Id));
    }

    @Test
    void getPost_NonExistentPost_ReturnsNotFound() throws Exception {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();

        // Act & Assert
        mockMvc.perform(get("/api/posts/{id}", nonExistentId)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePost_ValidRequest_ReturnsUpdatedPost() throws Exception {
        // Arrange
        // First create a post
        PostDto createRequest = PostDto.builder()
                .content("Original content")
                .build();

        String createResponse = mockMvc.perform(post("/api/posts")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PostDto createdPost = objectMapper.readValue(createResponse, PostDto.class);
        String postId = createdPost.getId().toString();

        // Update the post
        PostDto updateRequest = PostDto.builder()
                .content("Updated content with more details")
                .mediaUrl("https://example.com/updated.jpg")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/posts/{id}", postId)
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.content").value("Updated content with more details"))
                .andExpect(jsonPath("$.mediaUrl").value("https://example.com/updated.jpg"))
                .andExpect(jsonPath("$.authorId").value(user1Id));
    }

    @Test
    void updatePost_NotAuthor_ReturnsForbidden() throws Exception {
        // Arrange
        // User1 creates a post
        PostDto createRequest = PostDto.builder()
                .content("User1's post")
                .build();

        String createResponse = mockMvc.perform(post("/api/posts")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PostDto createdPost = objectMapper.readValue(createResponse, PostDto.class);
        String postId = createdPost.getId().toString();

        // User2 tries to update it
        PostDto updateRequest = PostDto.builder()
                .content("Unauthorized update")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/posts/{id}", postId)
                        .header("X-User-Id", user2Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletePost_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        // Create a post
        PostDto createRequest = PostDto.builder()
                .content("Post to delete")
                .build();

        String createResponse = mockMvc.perform(post("/api/posts")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PostDto createdPost = objectMapper.readValue(createResponse, PostDto.class);
        String postId = createdPost.getId().toString();

        // Act & Assert - Delete the post
        mockMvc.perform(delete("/api/posts/{id}", postId)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isOk());

        // Verify post is deleted
        mockMvc.perform(get("/api/posts/{id}", postId)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePost_NotAuthor_ReturnsForbidden() throws Exception {
        // Arrange
        // User1 creates a post
        PostDto createRequest = PostDto.builder()
                .content("User1's post")
                .build();

        String createResponse = mockMvc.perform(post("/api/posts")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PostDto createdPost = objectMapper.readValue(createResponse, PostDto.class);
        String postId = createdPost.getId().toString();

        // User2 tries to delete it
        // Act & Assert
        mockMvc.perform(delete("/api/posts/{id}", postId)
                        .header("X-User-Id", user2Id))
                .andExpect(status().isForbidden());
    }

    @Test
    void getFeed_ValidRequest_ReturnsPosts() throws Exception {
        // Arrange
        // Create multiple posts
        for (int i = 1; i <= 3; i++) {
            PostDto post = PostDto.builder()
                    .content("Post " + i + " from user1")
                    .build();
            mockMvc.perform(post("/api/posts")
                    .header("X-User-Id", user1Id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(post)));
        }

        for (int i = 1; i <= 2; i++) {
            PostDto post = PostDto.builder()
                    .content("Post " + i + " from user2")
                    .build();
            mockMvc.perform(post("/api/posts")
                    .header("X-User-Id", user2Id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(post)));
        }

        // Act & Assert - Get feed for user1
        mockMvc.perform(get("/api/posts/feed")
                        .header("X-User-Id", user1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5)) // All 5 posts
                .andExpect(jsonPath("$[0].content").exists())
                .andExpect(jsonPath("$[0].authorId").exists());
    }

    @Test
    void getFeed_WithPagination_ReturnsPage() throws Exception {
        // Arrange
        // Create multiple posts
        for (int i = 1; i <= 7; i++) {
            PostDto post = PostDto.builder()
                    .content("Post " + i)
                    .build();
            mockMvc.perform(post("/api/posts")
                    .header("X-User-Id", user1Id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(post)));
        }

        // Act & Assert - Get first page
        mockMvc.perform(get("/api/posts/feed")
                        .header("X-User-Id", user1Id)
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void likePost_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        // Create a post
        PostDto createRequest = PostDto.builder()
                .content("Post to like")
                .build();

        String createResponse = mockMvc.perform(post("/api/posts")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PostDto createdPost = objectMapper.readValue(createResponse, PostDto.class);
        String postId = createdPost.getId().toString();

        // Act & Assert - User2 likes the post
        mockMvc.perform(post("/api/posts/{id}/like", postId)
                        .header("X-User-Id", user2Id))
                .andExpect(status().isOk());

        // Verify likes count increased
        mockMvc.perform(get("/api/posts/{id}", postId)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likesCount").value(1));
    }

    @Test
    void likePost_AlreadyLiked_ReturnsBadRequest() throws Exception {
        // Arrange
        // Create a post
        PostDto createRequest = PostDto.builder()
                .content("Post to like")
                .build();

        String createResponse = mockMvc.perform(post("/api/posts")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PostDto createdPost = objectMapper.readValue(createResponse, PostDto.class);
        String postId = createdPost.getId().toString();

        // User2 likes the post
        mockMvc.perform(post("/api/posts/{id}/like", postId)
                .header("X-User-Id", user2Id));

        // Act & Assert - Try to like again
        mockMvc.perform(post("/api/posts/{id}/like", postId)
                        .header("X-User-Id", user2Id))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unlikePost_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        // Create a post
        PostDto createRequest = PostDto.builder()
                .content("Post to unlike")
                .build();

        String createResponse = mockMvc.perform(post("/api/posts")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PostDto createdPost = objectMapper.readValue(createResponse, PostDto.class);
        String postId = createdPost.getId().toString();

        // User2 likes the post
        mockMvc.perform(post("/api/posts/{id}/like", postId)
                .header("X-User-Id", user2Id));

        // Act & Assert - User2 unlikes the post
        mockMvc.perform(delete("/api/posts/{id}/like", postId)
                        .header("X-User-Id", user2Id))
                .andExpect(status().isOk());

        // Verify likes count decreased
        mockMvc.perform(get("/api/posts/{id}", postId)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likesCount").value(0));
    }

    @Test
    void unlikePost_NotLiked_ReturnsBadRequest() throws Exception {
        // Arrange
        // Create a post
        PostDto createRequest = PostDto.builder()
                .content("Post to unlike")
                .build();

        String createResponse = mockMvc.perform(post("/api/posts")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PostDto createdPost = objectMapper.readValue(createResponse, PostDto.class);
        String postId = createdPost.getId().toString();

        // Act & Assert - Try to unlike without liking first
        mockMvc.perform(delete("/api/posts/{id}/like", postId)
                        .header("X-User-Id", user2Id))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserPosts_ValidRequest_ReturnsUserPosts() throws Exception {
        // Arrange
        // Create posts for user1
        for (int i = 1; i <= 3; i++) {
            PostDto post = PostDto.builder()
                    .content("User1 post " + i)
                    .build();
            mockMvc.perform(post("/api/posts")
                    .header("X-User-Id", user1Id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(post)));
        }

        // Create posts for user2
        for (int i = 1; i <= 2; i++) {
            PostDto post = PostDto.builder()
                    .content("User2 post " + i)
                    .build();
            mockMvc.perform(post("/api/posts")
                    .header("X-User-Id", user2Id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(post)));
        }

        // Act & Assert - Get user1's posts
        mockMvc.perform(get("/api/posts/user/{userId}", user1Id)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].authorId").value(user1Id))
                .andExpect(jsonPath("$[1].authorId").value(user1Id))
                .andExpect(jsonPath("$[2].authorId").value(user1Id));
    }

    @Test
    void getUserPosts_WithPagination_ReturnsPage() throws Exception {
        // Arrange
        // Create multiple posts for user1
        for (int i = 1; i <= 8; i++) {
            PostDto post = PostDto.builder()
                    .content("User1 post " + i)
                    .build();
            mockMvc.perform(post("/api/posts")
                    .header("X-User-Id", user1Id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(post)));
        }

        // Act & Assert - Get first page
        mockMvc.perform(get("/api/posts/user/{userId}", user1Id)
                        .header("X-User-Id", user1Id)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }
}