package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Post;
import com.platform.model.dto.PostDto;
import com.platform.repository.PostRepository;
import com.platform.repository.ReactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private ReactionRepository reactionRepository;

    @Mock
    private CommentService commentService;

    @Mock
    private SecureMediaService secureMediaService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private PostService postService;

    private String userId;
    private UUID userIdUuid;
    private String postId;
    private String authorName;
    private String authorRole;

    @BeforeEach
    void setUp() {
        userId = "user-123";
        userIdUuid = UUID.fromString(userId);
        postId = "post-456";
        authorName = "TestAuthor";
        authorRole = "USER_1";
    }

    @Test
    void createPost_SavesPostWithCorrectFields() {
        // Arrange
        String content = "Test post content";
        String mediaId = "media-789";

        when(jwt.getSubject()).thenReturn(userId);
        when(jwt.getClaim("preferred_username")).thenReturn(authorName);
        when(jwt.getClaim("realm_access")).thenReturn(java.util.Map.of("roles", List.of(authorRole)));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post post = inv.getArgument(0);
            post.setId(postId);
            return post;
        });

        // Act
        PostDto result = postService.createPost(content, mediaId, jwt);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getAuthorId()).isEqualTo(userIdUuid);
        assertThat(result.getAuthorName()).isEqualTo(authorName);
        assertThat(result.getAuthorRole()).isEqualTo(authorRole);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void updatePost_ByOwner_UpdatesSuccessfully() {
        // Arrange
        String newContent = "Updated content";
        String newMediaId = "new-media-123";

        Post existingPost = Post.builder()
                .id(postId)
                .authorId(userIdUuid)
                .content("Old content")
                .mediaId("old-media-456")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenReturn(existingPost);

        // Act
        PostDto result = postService.updatePost(postId, newContent, newMediaId, userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(newContent);
        assertThat(result.getMediaId()).isEqualTo(newMediaId);
        verify(postRepository).save(existingPost);
    }

    @Test
    void updatePost_ByAdmin_UpdatesSuccessfully() {
        // Arrange
        String newContent = "Updated by admin";
        String adminUserId = "admin-789";
        UUID adminUserIdUuid = UUID.fromString(adminUserId);

        Post existingPost = Post.builder()
                .id(postId)
                .authorId(userIdUuid)  // Different author
                .content("Old content")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenReturn(existingPost);

        // Mock admin check
        when(postService.isAdmin(adminUserId)).thenReturn(true);

        // Act
        PostDto result = postService.updatePost(postId, newContent, null, adminUserId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(newContent);
        verify(postRepository).save(existingPost);
    }

    @Test
    void updatePost_ByOtherUser_ThrowsException() {
        // Arrange
        String otherUserId = "other-999";
        Post existingPost = Post.builder()
                .id(postId)
                .authorId(userIdUuid)  // Different author
                .content("Old content")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postService.isAdmin(otherUserId)).thenReturn(false);
        when(postService.isModerator(otherUserId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> postService.updatePost(postId, "New content", null, otherUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void deletePost_ByOwner_PerformsSoftDelete() {
        // Arrange
        Post existingPost = Post.builder()
                .id(postId)
                .authorId(userIdUuid)
                .content("Test post")
                .deletedAt(null)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenReturn(existingPost);

        // Act
        postService.deletePost(postId, userId);

        // Assert
        assertThat(existingPost.getDeletedAt()).isNotNull();
        verify(postRepository).save(existingPost);
    }

    @Test
    void deletePost_ByAdmin_PerformsSoftDelete() {
        // Arrange
        String adminUserId = "admin-789";
        Post existingPost = Post.builder()
                .id(postId)
                .authorId(userIdUuid)  // Different author
                .content("Test post")
                .deletedAt(null)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenReturn(existingPost);
        when(postService.isAdmin(adminUserId)).thenReturn(true);

        // Act
        postService.deletePost(postId, adminUserId);

        // Assert
        assertThat(existingPost.getDeletedAt()).isNotNull();
        verify(postRepository).save(existingPost);
    }

    @Test
    void deletePost_ByOtherUser_ThrowsException() {
        // Arrange
        String otherUserId = "other-999";
        Post existingPost = Post.builder()
                .id(postId)
                .authorId(userIdUuid)  // Different author
                .content("Test post")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postService.isAdmin(otherUserId)).thenReturn(false);
        when(postService.isModerator(otherUserId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> postService.deletePost(postId, otherUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void getPost_Found_ReturnsDtoWithUserLikedAndComments() {
        // Arrange
        Post existingPost = Post.builder()
                .id(postId)
                .authorId(userIdUuid)
                .content("Test post")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(reactionRepository.existsByPostIdAndUserId(postId, userIdUuid)).thenReturn(true);
        when(commentService.getRecentComments(postId, 5)).thenReturn(List.of());

        // Act
        PostDto result = postService.getPost(postId, userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(postId);
        assertThat(result.getUserLiked()).isTrue();
        assertThat(result.getRecentComments()).isEmpty();
    }

    @Test
    void getPost_NotFound_ThrowsException() {
        // Arrange
        String nonExistentPostId = "non-existent-post";
        when(postRepository.findById(nonExistentPostId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> postService.getPost(nonExistentPostId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Post not found");
    }

    @Test
    void getFeed_WithBefore_DelegatesToRepository() {
        // Arrange
        String before = "2024-01-15T10:30:00Z";
        LocalDateTime beforeDateTime = LocalDateTime.parse("2024-01-15T10:30:00");

        Post post = Post.builder()
                .id("feed-post-1")
                .authorId(userIdUuid)
                .content("Feed post")
                .build();

        when(postRepository.findFeedBefore(beforeDateTime, 20)).thenReturn(List.of(post));
        when(reactionRepository.existsByPostIdAndUserId("feed-post-1", userIdUuid)).thenReturn(false);
        when(commentService.getRecentComments("feed-post-1", 5)).thenReturn(List.of());

        // Act
        List<PostDto> result = postService.getFeed(before, userId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("feed-post-1");
        verify(postRepository).findFeedBefore(beforeDateTime, 20);
    }

    @Test
    void getFeed_WithoutBefore_DelegatesToRepository() {
        // Arrange
        Post post = Post.builder()
                .id("feed-post-2")
                .authorId(userIdUuid)
                .content("Latest feed post")
                .build();

        when(postRepository.findLatestFeed(20)).thenReturn(List.of(post));
        when(reactionRepository.existsByPostIdAndUserId("feed-post-2", userIdUuid)).thenReturn(false);
        when(commentService.getRecentComments("feed-post-2", 5)).thenReturn(List.of());

        // Act
        List<PostDto> result = postService.getFeed(null, userId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("feed-post-2");
        verify(postRepository).findLatestFeed(20);
    }
}