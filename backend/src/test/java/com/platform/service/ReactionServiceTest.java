package com.platform.service;

import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Post;
import com.platform.model.Reaction;
import com.platform.model.dto.ReactionDto;
import com.platform.repository.PostRepository;
import com.platform.repository.ReactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactionService Unit Tests")
class ReactionServiceTest {

    @Mock
    private ReactionRepository reactionRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private ReactionService reactionService;

    private Post testPost;
    private Reaction testReaction;
    private Jwt testJwt;

    @BeforeEach
    void setUp() {
        testPost = Post.builder()
                .id("post-1")
                .content("Test Post Content")
                .likesCount(10)
                .build();

        testReaction = Reaction.builder()
                .id("reaction-1")
                .postId("post-1")
                .userId("user-1")
                .type("like")
                .createdAt(LocalDateTime.now())
                .build();

        testJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-1")
                .claim("preferred_username", "testuser")
                .claim("realm_access.roles", "USER")
                .build();
    }

    @Test
    @DisplayName("toggleReaction - add reaction when not exists")
    void toggleReaction_Add_ShouldAddAndIncrement() {
        when(postRepository.findById("post-1")).thenReturn(Optional.of(testPost));
        when(reactionRepository.findByPostIdAndUserIdAndType("post-1", "user-1", "like"))
                .thenReturn(Optional.empty());
        when(reactionRepository.save(any(Reaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        ReactionDto result = reactionService.toggleReaction("post-1", "like", testJwt);

        assertThat(result.getUserReacted()).isTrue();
        assertThat(result.getCount()).isEqualTo(11L);
        assertThat(result.getType()).isEqualTo("like");
        verify(reactionRepository).save(any(Reaction.class));
        verify(postRepository).save(testPost);
        assertThat(testPost.getLikesCount()).isEqualTo(11);
    }

    @Test
    @DisplayName("toggleReaction - remove reaction when exists")
    void toggleReaction_Remove_ShouldRemoveAndDecrement() {
        when(postRepository.findById("post-1")).thenReturn(Optional.of(testPost));
        when(reactionRepository.findByPostIdAndUserIdAndType("post-1", "user-1", "like"))
                .thenReturn(Optional.of(testReaction));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        ReactionDto result = reactionService.toggleReaction("post-1", "like", testJwt);

        assertThat(result.getUserReacted()).isFalse();
        assertThat(result.getCount()).isEqualTo(9L);
        verify(reactionRepository).delete(testReaction);
        assertThat(testPost.getLikesCount()).isEqualTo(9);
    }

    @Test
    @DisplayName("toggleReaction - remove does not go below zero")
    void toggleReaction_Remove_AtZero_ShouldStayAtZero() {
        testPost.setLikesCount(0);
        when(postRepository.findById("post-1")).thenReturn(Optional.of(testPost));
        when(reactionRepository.findByPostIdAndUserIdAndType("post-1", "user-1", "like"))
                .thenReturn(Optional.of(testReaction));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        ReactionDto result = reactionService.toggleReaction("post-1", "like", testJwt);

        assertThat(result.getCount()).isEqualTo(0L);
        assertThat(testPost.getLikesCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("toggleReaction - post not found throws exception")
    void toggleReaction_PostNotFound_ShouldThrow() {
        when(postRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reactionService.toggleReaction("nonexistent", "like", testJwt))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Post not found");
    }

    @Test
    @DisplayName("getReactionStatus - returns status with user reacted true")
    void getReactionStatus_UserReacted_ShouldReturnTrue() {
        when(reactionRepository.countByPostIdAndType("post-1", "like")).thenReturn(5L);
        when(reactionRepository.existsByPostIdAndUserIdAndType("post-1", "user-1", "like"))
                .thenReturn(true);

        ReactionDto result = reactionService.getReactionStatus("post-1", testJwt);

        assertThat(result.getCount()).isEqualTo(5L);
        assertThat(result.getUserReacted()).isTrue();
        assertThat(result.getPostId()).isEqualTo("post-1");
        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getType()).isEqualTo("like");
    }

    @Test
    @DisplayName("getReactionStatus - returns status with user reacted false")
    void getReactionStatus_UserNotReacted_ShouldReturnFalse() {
        when(reactionRepository.countByPostIdAndType("post-1", "like")).thenReturn(3L);
        when(reactionRepository.existsByPostIdAndUserIdAndType("post-1", "user-1", "like"))
                .thenReturn(false);

        ReactionDto result = reactionService.getReactionStatus("post-1", testJwt);

        assertThat(result.getCount()).isEqualTo(3L);
        assertThat(result.getUserReacted()).isFalse();
    }

    @Test
    @DisplayName("getReactionStatus - zero reactions")
    void getReactionStatus_ZeroReactions_ShouldReturnZero() {
        when(reactionRepository.countByPostIdAndType("post-1", "like")).thenReturn(0L);
        when(reactionRepository.existsByPostIdAndUserIdAndType("post-1", "user-1", "like"))
                .thenReturn(false);

        ReactionDto result = reactionService.getReactionStatus("post-1", testJwt);

        assertThat(result.getCount()).isEqualTo(0L);
        assertThat(result.getUserReacted()).isFalse();
    }
}
