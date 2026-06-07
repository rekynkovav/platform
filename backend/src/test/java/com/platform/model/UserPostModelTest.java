package com.platform.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserPostModelTest {

    // ==================== User ====================

    @Test
    void user_Builder_SetsAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        User user = User.builder()
                .id(id)
                .username("testuser")
                .email("test@example.com")
                .phone("+1234567890")
                .role("USER")
                .avatarUrl("https://example.com/avatar.png")
                .bio("Hello world")
                .lastActiveAt(now)
                .banned(false)
                .banReason(null)
                .bannedAt(null)
                .createdAt(now)
                .updatedAt(now)
                .version(1L)
                .build();

        assertEquals(id, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("+1234567890", user.getPhone());
        assertEquals("USER", user.getRole());
        assertEquals("https://example.com/avatar.png", user.getAvatarUrl());
        assertEquals("Hello world", user.getBio());
        assertFalse(user.isBanned());
        assertEquals(1L, user.getVersion());
    }

    @Test
    void user_DefaultConstructor_CreatesInstance() {
        User user = new User();
        assertNull(user.getId());
        assertNull(user.getUsername());
        assertFalse(user.isBanned());
    }

    @Test
    void user_SettersAndGettersWork() {
        User user = new User();
        user.setUsername("admin");
        user.setRole("SUPER_ADMIN");
        user.setBanned(true);
        user.setBanReason("spam");

        assertEquals("admin", user.getUsername());
        assertEquals("SUPER_ADMIN", user.getRole());
        assertTrue(user.isBanned());
        assertEquals("spam", user.getBanReason());
    }

    // ==================== Post ====================

    @Test
    void post_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();

        Post post = Post.builder()
                .id("post-1")
                .content("Test content")
                .authorId("author-1")
                .authorName("John")
                .authorRole("USER")
                .category("general")
                .likesCount(5)
                .commentsCount(2)
                .sharesCount(1)
                .viewsCount(100)
                .flagged(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals("post-1", post.getId());
        assertEquals("Test content", post.getContent());
        assertEquals("author-1", post.getAuthorId());
        assertEquals("general", post.getCategory());
        assertEquals(5, post.getLikesCount());
        assertEquals(100, post.getViewsCount());
        assertFalse(post.isFlagged());
    }

    @Test
    void post_DefaultConstructor_InitializesCountersToZero() {
        Post post = new Post();
        assertEquals(0, post.getLikesCount());
        assertEquals(0, post.getCommentsCount());
        assertEquals(0, post.getSharesCount());
        assertEquals(0, post.getViewsCount());
        assertNotNull(post.getComments());
        assertNotNull(post.getReactions());
        assertTrue(post.getComments().isEmpty());
        assertTrue(post.getReactions().isEmpty());
    }

    @Test
    void post_ModerationFields_WorkCorrectly() {
        Post post = new Post();
        post.setFlagged(true);
        post.setModeratedAt(LocalDateTime.now());
        post.setModeratedBy("moderator-1");
        post.setModerationReason("inappropriate content");

        assertTrue(post.isFlagged());
        assertNotNull(post.getModeratedAt());
        assertEquals("moderator-1", post.getModeratedBy());
        assertEquals("inappropriate content", post.getModerationReason());
    }
}
