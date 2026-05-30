package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.model.dto.PostDto;
import com.platform.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PostService postService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private PostController postController;

    private String userId;
    private UUID userIdUuid;
    private String postId;
    private UUID postIdUuid;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postController).build();
        objectMapper = new ObjectMapper();

        userId = "user-123";
        userIdUuid = UUID.fromString(userId);
        postId = "post-456";
        postIdUuid = UUID.fromString(postId);
    }

    @Test
    void createPost_ValidRequest_ReturnsPostDto() throws Exception {
        // Arrange
        String content = "Test post content";
        String mediaId = "media-789";
        PostDto expectedResponse = PostDto.builder()
                .id(postIdUuid)
                .content(content)
                .authorId(userIdUuid)
                .authorName("TestAuthor")
                .authorRole("USER_1")
                .mediaId(UUID.fromString(mediaId))
                .createdAt(Instant.now())
                .build();

        when(jwt.getSubject()).thenReturn(userId);
        when(jwt.getClaim("preferred_username")).thenReturn("TestAuthor");
        when(jwt.getClaim("realm_access")).thenReturn(java.util.Map.of("roles", List.of("USER_1")));
        when(postService.createPost(eq(content), eq(mediaId), eq(jwt)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/posts")
                        .with(request -> {
                            request.setAttribute("jwt", jwt);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + content + "\", \"mediaId\": \"" + mediaId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.authorId").value(userId))
                .andExpect(jsonPath("$.authorName").value("TestAuthor"))
                .andExpect(jsonPath("$.mediaId").value(mediaId));
    }

    @Test
    void createPost_WithoutMedia_ReturnsPostDto() throws Exception {
        // Arrange
        String content = "Test post without media";
        PostDto expectedResponse = PostDto.builder()
                .id(postIdUuid)
                .content(content)
                .authorId(userIdUuid)
                .authorName("TestAuthor")
                .authorRole("USER_1")
                .createdAt(Instant.now())
                .build();

        when(jwt.getSubject()).thenReturn(userId);
        when(jwt.getClaim("preferred_username")).thenReturn("TestAuthor");
        when(jwt.getClaim("realm_access")).thenReturn(java.util.Map.of("roles", List.of("USER_1")));
        when(postService.createPost(eq(content), isNull(), eq(jwt)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/posts")
                        .with(request -> {
                            request.setAttribute("jwt", jwt);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + content + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaId").doesNotExist());
    }

    @Test
    void createPost_EmptyContent_ReturnsBadRequest() throws Exception {
        // Arrange
        String emptyContent = "";

        // Act & Assert
        mockMvc.perform(post("/api/posts")
                        .with(request -> {
                            request.setAttribute("jwt", jwt);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + emptyContent + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatePost_ValidRequest_ReturnsUpdatedPost() throws Exception {
        // Arrange
        String newContent = "Updated content";
        String newMediaId = "new-media-123";
        PostDto expectedResponse = PostDto.builder()
                .id(postIdUuid)
                .content(newContent)
                .authorId(userIdUuid)
                .authorName("TestAuthor")
                .mediaId(UUID.fromString(newMediaId))
                .updatedAt(Instant.now())
                .build();

        when(postService.updatePost(eq(postId), eq(newContent), eq(newMediaId), eq(userId)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(put("/api/posts/{id}", postId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + newContent + "\", \"mediaId\": \"" + newMediaId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(newContent))
                .andExpect(jsonPath("$.mediaId").value(newMediaId));
    }

    @Test
    void updatePost_WithoutMedia_ReturnsUpdatedPost() throws Exception {
        // Arrange
        String newContent = "Updated content without media";
        PostDto expectedResponse = PostDto.builder()
                .id(postIdUuid)
                .content(newContent)
                .authorId(userIdUuid)
                .authorName("TestAuthor")
                .updatedAt(Instant.now())
                .build();

        when(postService.updatePost(eq(postId), eq(newContent), isNull(), eq(userId)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(put("/api/posts/{id}", postId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + newContent + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaId").doesNotExist());
    }

    @Test
    void deletePost_ValidRequest_ReturnsNoContent() throws Exception {
        // Arrange
        when(postService.deletePost(eq(postId), eq(userId))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/posts/{id}", postId)
                        .header("X-User-Id", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePost_PostNotFound_ReturnsNotFound() throws Exception {
        // Arrange
        String nonExistentPostId = "non-existent-post";
        when(postService.deletePost(eq(nonExistentPostId), eq(userId)))
                .thenThrow(new com.platform.exception.ResourceNotFoundException("Post not found"));

        // Act & Assert
        mockMvc.perform(delete("/api/posts/{id}", nonExistentPostId)
                        .header("X-User-Id", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPost_ValidRequest_ReturnsPostDto() throws Exception {
        // Arrange
        PostDto expectedResponse = PostDto.builder()
                .id(postIdUuid)
                .content("Test post content")
                .authorId(userIdUuid)
                .authorName("TestAuthor")
                .authorRole("USER_1")
                .likesCount(15)
                .userLiked(true)
                .createdAt(Instant.now().minusHours(2))
                .build();

        when(postService.getPost(eq(postId), eq(userId))).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/posts/{id}", postId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.likesCount").value(15))
                .andExpect(jsonPath("$.userLiked").value(true));
    }

    @Test
    void getPost_WithoutUserId_ReturnsPostDto() throws Exception {
        // Arrange
        PostDto expectedResponse = PostDto.builder()
                .id(postIdUuid)
                .content("Test post content")
                .authorId(userIdUuid)
                .authorName("TestAuthor")
                .authorRole("USER_1")
                .likesCount(15)
                .userLiked(false) // No userId provided
                .createdAt(Instant.now().minusHours(2))
                .build();

        when(postService.getPost(eq(postId), isNull())).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userLiked").value(false));
    }

    @Test
    void getFeed_ValidRequest_ReturnsPosts() throws Exception {
        // Arrange
        String before = "2024-01-15T10:30:00Z";
        PostDto post1 = PostDto.builder()
                .id(UUID.randomUUID())
                .content("Post 1")
                .authorId(userIdUuid)
                .authorName("User1")
                .likesCount(10)
                .createdAt(Instant.parse(before).minusSeconds(300))
                .build();

        PostDto post2 = PostDto.builder()
                .id(UUID.randomUUID())
                .content("Post 2")
                .authorId(UUID.randomUUID())
                .authorName("User2")
                .likesCount(5)
                .createdAt(Instant.parse(before).minusSeconds(150))
                .build();

        List<PostDto> expectedResponse = List.of(post1, post2);

        when(postService.getFeed(eq(before), eq(userId))).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/posts/feed")
                        .header("X-User-Id", userId)
                        .param("before", before))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].likesCount").value(10))
                .andExpect(jsonPath("$[1].likesCount").value(5));
    }

    @Test
    void getFeed_WithoutBefore_ReturnsLatestPosts() throws Exception {
        // Arrange
        PostDto post = PostDto.builder()
                .id(UUID.randomUUID())
                .content("Latest post")
                .authorId(userIdUuid)
                .authorName("User1")
                .likesCount(20)
                .createdAt(Instant.now().minusMinutes(30))
                .build();

        List<PostDto> expectedResponse = List.of(post);

        when(postService.getFeed(isNull(), eq(userId))).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/posts/feed")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].likesCount").value(20));
    }

    @Test
    void getFeed_WithoutUserId_ReturnsPosts() throws Exception {
        // Arrange
        PostDto post = PostDto.builder()
                .id(UUID.randomUUID())
                .content("Public post")
                .authorId(userIdUuid)
                .authorName("User1")
                .likesCount(25)
                .userLiked(false) // No userId
                .createdAt(Instant.now().minusMinutes(30))
                .build();

        List<PostDto> expectedResponse = List.of(post);

        when(postService.getFeed(isNull(), isNull())).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/posts/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userLiked").value(false));
    }

    @Test
    void likePost_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        when(postService.likePost(eq(postId), eq(userId))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/posts/{id}/like", postId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk());
    }

    @Test
    void unlikePost_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        when(postService.unlikePost(eq(postId), eq(userId))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/posts/{id}/like", postId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk());
    }
}