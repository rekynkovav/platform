package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Comment;
import com.platform.model.Post;
import com.platform.model.dto.CommentDto;
import com.platform.repository.CommentRepository;
import com.platform.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService Unit Tests")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private CommentService commentService;

    private Post testPost;
    private Comment testComment;
    private Jwt testJwt;

    @BeforeEach
    void setUp() {
        testPost = Post.builder()
                .id("post-1")
                .content("Test Post Content")
                .commentsCount(5)
                .build();

        testComment = Comment.builder()
                .id("comment-1")
                .content("Test comment content")
                .postId("post-1")
                .authorId("user-1")
                .authorName("testuser")
                .createdAt(LocalDateTime.now())
                .build();

        testJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-1")
                .claim("preferred_username", "testuser")
                .claim("realm_access", Map.of("roles", List.of("USER")))
                .build();
    }

    @Test
    @DisplayName("getCommentsByPost - should return comments list")
    void getCommentsByPost_ShouldReturnList() {
        when(commentRepository.findByPostIdOrderByCreatedAtDesc(eq("post-1"), any(Pageable.class)))
                .thenReturn(List.of(testComment));

        List<CommentDto> result = commentService.getCommentsByPost("post-1", 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Test comment content");
        assertThat(result.get(0).getAuthorName()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("getCommentsByPost - empty list when no comments")
    void getCommentsByPost_Empty_ShouldReturnEmptyList() {
        when(commentRepository.findByPostIdOrderByCreatedAtDesc(eq("post-1"), any(Pageable.class)))
                .thenReturn(List.of());

        List<CommentDto> result = commentService.getCommentsByPost("post-1", 10);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getRecentComments - delegates to getCommentsByPost")
    void getRecentComments_ShouldDelegate() {
        when(commentRepository.findByPostIdOrderByCreatedAtDesc(eq("post-1"), any(Pageable.class)))
                .thenReturn(List.of(testComment));

        List<CommentDto> result = commentService.getRecentComments("post-1", 5);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getTotalCount - returns repository count")
    void getTotalCount_ShouldReturnCount() {
        when(commentRepository.count()).thenReturn(42L);

        long count = commentService.getTotalCount();

        assertThat(count).isEqualTo(42L);
    }

    @Test
    @DisplayName("createComment - should create comment and increment post counter")
    void createComment_ShouldCreateAndIncrementCounter() {
        CommentDto.CreateRequest request = CommentDto.CreateRequest.builder()
                .content("New comment")
                .build();

        when(postRepository.findById("post-1")).thenReturn(Optional.of(testPost));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId("new-id");
            return c;
        });
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        CommentDto result = commentService.createComment("post-1", request, testJwt);

        assertThat(result.getContent()).isEqualTo("New comment");
        assertThat(result.getAuthorId()).isEqualTo("user-1");
        assertThat(result.getAuthorName()).isEqualTo("testuser");
        assertThat(testPost.getCommentsCount()).isEqualTo(6);
        verify(commentRepository).save(any(Comment.class));
        verify(postRepository).save(testPost);
    }

    @Test
    @DisplayName("createComment - post not found throws exception")
    void createComment_PostNotFound_ShouldThrow() {
        CommentDto.CreateRequest request = CommentDto.CreateRequest.builder()
                .content("New comment")
                .build();

        when(postRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment("nonexistent", request, testJwt))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Post not found");
    }

    @Test
    @DisplayName("deleteComment - author can delete own comment")
    void deleteComment_ByAuthor_ShouldDelete() {
        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(testComment));
        when(postRepository.findById("post-1")).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        commentService.deleteComment("post-1", "comment-1", testJwt);

        verify(commentRepository).delete(testComment);
        assertThat(testPost.getCommentsCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("deleteComment - admin can delete any comment")
    void deleteComment_ByAdmin_ShouldDelete() {
        Jwt adminJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("admin-1")
                .claim("preferred_username", "admin")
                .claim("realm_access", Map.of("roles", List.of("MODERATOR_ADMIN")))
                .build();

        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(testComment));
        when(postRepository.findById("post-1")).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        commentService.deleteComment("post-1", "comment-1", adminJwt);

        verify(commentRepository).delete(testComment);
    }

    @Test
    @DisplayName("deleteComment - non-author non-admin throws AccessDeniedException")
    void deleteComment_ByOtherUser_ShouldThrowAccessDenied() {
        Jwt otherJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-2")
                .claim("preferred_username", "other")
                .claim("realm_access", Map.of("roles", List.of("USER")))
                .build();

        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(testComment));

        assertThatThrownBy(() -> commentService.deleteComment("post-1", "comment-1", otherJwt))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permission");
    }

    @Test
    @DisplayName("deleteComment - comment not found throws exception")
    void deleteComment_CommentNotFound_ShouldThrow() {
        when(commentRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment("post-1", "nonexistent", testJwt))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Comment not found");
    }

    @Test
    @DisplayName("deleteComment - comment belongs to different post throws exception")
    void deleteComment_WrongPost_ShouldThrow() {
        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(testComment));

        assertThatThrownBy(() -> commentService.deleteComment("post-2", "comment-1", testJwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    @DisplayName("deleteComment - counter does not go below zero")
    void deleteComment_CounterAtZero_ShouldNotGoNegative() {
        testPost.setCommentsCount(0);
        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(testComment));
        when(postRepository.findById("post-1")).thenReturn(Optional.of(testPost));

        commentService.deleteComment("post-1", "comment-1", testJwt);

        assertThat(testPost.getCommentsCount()).isEqualTo(0);
        verify(postRepository, never()).save(any());
    }
}
