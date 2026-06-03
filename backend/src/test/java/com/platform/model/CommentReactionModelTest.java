package com.platform.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CommentReactionModelTest {

    // ==================== Comment ====================

    @Test
    void comment_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();

        Comment comment = Comment.builder()
                .id("comment-1")
                .content("Great post!")
                .postId("post-1")
                .authorId("user-1")
                .authorName("Alice")
                .createdAt(now)
                .build();

        assertEquals("comment-1", comment.getId());
        assertEquals("Great post!", comment.getContent());
        assertEquals("post-1", comment.getPostId());
        assertEquals("user-1", comment.getAuthorId());
        assertEquals("Alice", comment.getAuthorName());
        assertEquals(now, comment.getCreatedAt());
    }

    @Test
    void comment_DefaultConstructor_CreatesInstance() {
        Comment comment = new Comment();
        assertNull(comment.getId());
        assertNull(comment.getContent());
        assertNull(comment.getPost());
    }

    @Test
    void comment_SettersAndGettersWork() {
        Comment comment = new Comment();
        comment.setContent("Updated comment");
        comment.setAuthorId("user-2");
        comment.setAuthorName("Bob");

        assertEquals("Updated comment", comment.getContent());
        assertEquals("user-2", comment.getAuthorId());
        assertEquals("Bob", comment.getAuthorName());
    }

    // ==================== Reaction ====================

    @Test
    void reaction_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();

        Reaction reaction = Reaction.builder()
                .id("reaction-1")
                .postId("post-1")
                .userId("user-1")
                .type("like")
                .createdAt(now)
                .build();

        assertEquals("reaction-1", reaction.getId());
        assertEquals("post-1", reaction.getPostId());
        assertEquals("user-1", reaction.getUserId());
        assertEquals("like", reaction.getType());
        assertEquals(now, reaction.getCreatedAt());
    }

    @Test
    void reaction_DefaultConstructor_CreatesInstance() {
        Reaction reaction = new Reaction();
        assertNull(reaction.getId());
        assertNull(reaction.getType());
        assertNull(reaction.getPost());
    }

    @Test
    void reaction_SettersAndGettersWork() {
        Reaction reaction = new Reaction();
        reaction.setType("love");
        reaction.setUserId("user-3");
        reaction.setPostId("post-2");

        assertEquals("love", reaction.getType());
        assertEquals("user-3", reaction.getUserId());
        assertEquals("post-2", reaction.getPostId());
    }
}
