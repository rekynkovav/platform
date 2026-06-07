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
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
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

    @InjectMocks
    private PostService postService;

    private Jwt jwt;
    private String userId;

    @BeforeEach
    void setUp() {
        userId = "user-123";
        jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("preferred_username", "testuser")
                .claim("roles", List.of("USER_1"))
                .build();
    }

    @Test
    void getFeed_ReturnsPosts() {
        Post post = Post.builder()
                .id("post-1")
                .content("Test content")
                .authorId("author-1")
                .authorName("Alice")
                .category("general")
                .likesCount(0)
                .commentsCount(0)
                .sharesCount(0)
                .viewsCount(0)
                .flagged(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(postRepository.findFeed(any(PageRequest.class))).thenReturn(List.of(post));
        when(reactionRepository.existsByPostIdAndUserIdAndType(anyString(), anyString(), eq("like"))).thenReturn(false);
        when(commentService.getRecentComments(anyString(), anyInt())).thenReturn(List.of());

        List<PostDto> feed = postService.getFeed(userId, null, PageRequest.of(0, 10));

        assertThat(feed).hasSize(1);
        assertThat(feed.get(0).getContent()).isEqualTo("Test content");
    }

    @Test
    void getFeedByCategory_ReturnsFilteredPosts() {
        Post post = Post.builder()
                .id("post-2")
                .content("Nutrition post")
                .authorId("author-1")
                .authorName("Alice")
                .category("nutrition")
                .likesCount(5)
                .commentsCount(2)
                .sharesCount(1)
                .viewsCount(100)
                .flagged(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(postRepository.findFeedByCategory(eq("nutrition"), any(PageRequest.class))).thenReturn(List.of(post));
        when(reactionRepository.existsByPostIdAndUserIdAndType(anyString(), anyString(), eq("like"))).thenReturn(true);
        when(commentService.getRecentComments(anyString(), anyInt())).thenReturn(List.of());

        List<PostDto> feed = postService.getFeedByCategory("nutrition", userId, null, PageRequest.of(0, 10));

        assertThat(feed).hasSize(1);
        assertThat(feed.get(0).getUserLiked()).isTrue();
    }

    @Test
    void getPost_ExistingPost_ReturnsPost() {
        Post post = Post.builder()
                .id("post-3")
                .content("Specific post")
                .authorId("author-1")
                .authorName("Bob")
                .category("sport")
                .likesCount(10)
                .commentsCount(3)
                .sharesCount(0)
                .viewsCount(50)
                .flagged(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(postRepository.findById("post-3")).thenReturn(Optional.of(post));
        when(reactionRepository.existsByPostIdAndUserIdAndType(anyString(), anyString(), eq("like"))).thenReturn(false);
        when(commentService.getRecentComments(anyString(), anyInt())).thenReturn(List.of());

        PostDto result = postService.getPost("post-3", userId);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Specific post");
        assertThat(result.getAuthorName()).isEqualTo("Bob");
    }

    @Test
    void getPost_NonExistentPost_ThrowsException() {
        when(postRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost("nonexistent", userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createPost_SavesAndReturnsPost() {
        PostDto.CreateRequest request = PostDto.CreateRequest.builder()
                .content("New post content")
                .category("general")
                .build();

        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reactionRepository.existsByPostIdAndUserIdAndType(anyString(), anyString(), eq("like"))).thenReturn(false);
        when(commentService.getRecentComments(anyString(), anyInt())).thenReturn(List.of());

        PostDto result = postService.createPost(request, jwt);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("New post content");
        assertThat(result.getAuthorId()).isEqualTo(userId);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void deletePost_AuthorCanDelete() {
        Post post = Post.builder()
                .id("post-del")
                .content("To delete")
                .authorId(userId)
                .authorName("testuser")
                .category("general")
                .build();

        when(postRepository.findById("post-del")).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        postService.deletePost("post-del", jwt);

        verify(postRepository).save(argThat(p -> p.getDeletedAt() != null));
    }

    @Test
    void deletePost_NonAuthorNonAdmin_ThrowsException() {
        Post post = Post.builder()
                .id("post-del2")
                .content("Protected")
                .authorId("other-user")
                .authorName("Other")
                .category("general")
                .build();

        when(postRepository.findById("post-del2")).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost("post-del2", jwt))
                .isInstanceOf(AccessDeniedException.class);

        verify(postRepository, never()).save(any());
    }
}
