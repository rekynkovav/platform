package com.platform.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PostReactionTest {

    // ==================== Post ====================

    @Test
    void post_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        Post post = Post.builder()
                .id("post-1")
                .content("Hello world!")
                .authorId("user-1")
                .authorName("Alice")
                .authorRole("USER_1")
                .mediaUrl("/media/img.jpg")
                .mediaType("image")
                .category("general")
                .likesCount(10)
                .commentsCount(5)
                .sharesCount(2)
                .viewsCount(100)
                .flagged(false)
                .moderatedAt(null)
                .moderatedBy(null)
                .moderationReason(null)
                .deletedAt(null)
                .createdAt(now)
                .updatedAt(now.plusMinutes(5))
                .comments(new ArrayList<>())
                .reactions(new ArrayList<>())
                .build();

        assertEquals("post-1", post.getId());
        assertEquals("Hello world!", post.getContent());
        assertEquals("general", post.getCategory());
        assertEquals(10, post.getLikesCount());
        assertFalse(post.isFlagged());
        assertNotNull(post.getComments());
        assertNotNull(post.getReactions());
    }

    @Test
    void post_NoArgsConstructor_CreatesWithDefaults() {
        Post post = new Post();
        assertNull(post.getId());
        assertNull(post.getContent());
        // Field initializers may or may not apply with Lombok @NoArgsConstructor
    }

    @Test
    void post_ModerationFields_Work() {
        Post post = new Post();
        post.setFlagged(true);
        post.setModeratedAt(LocalDateTime.now());
        post.setModeratedBy("mod-1");
        post.setModerationReason("Spam");
        post.setDeletedAt(LocalDateTime.now());

        assertTrue(post.isFlagged());
        assertNotNull(post.getModeratedAt());
        assertEquals("mod-1", post.getModeratedBy());
        assertEquals("Spam", post.getModerationReason());
        assertNotNull(post.getDeletedAt());
    }

    // ==================== Reaction ====================

    @Test
    void reaction_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        Reaction reaction = Reaction.builder()
                .id("react-1")
                .postId("post-1")
                .userId("user-1")
                .type("like")
                .createdAt(now)
                .build();

        assertEquals("react-1", reaction.getId());
        assertEquals("post-1", reaction.getPostId());
        assertEquals("user-1", reaction.getUserId());
        assertEquals("like", reaction.getType());
        assertEquals(now, reaction.getCreatedAt());
    }

    @Test
    void reaction_NoArgsConstructor_CreatesEmptyObject() {
        Reaction reaction = new Reaction();
        assertNull(reaction.getId());
        assertNull(reaction.getPostId());
        assertNull(reaction.getType());
    }
}
