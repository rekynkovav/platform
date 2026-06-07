package com.platform.model.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class QuestionReactionDtoTest {

    // ==================== QuestionDto ====================

    @Test
    void questionDto_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        QuestionDto dto = QuestionDto.builder()
                .id("q-1")
                .question("How does this work?")
                .answer("It works like this...")
                .authorId("user-1")
                .authorName("Alice")
                .mode("public")
                .recipientId("expert-1")
                .isAnswered(true)
                .createdAt(now.minusHours(1))
                .answeredAt(now)
                .answeredBy("expert-1")
                .build();

        assertEquals("q-1", dto.getId());
        assertEquals("How does this work?", dto.getQuestion());
        assertEquals("It works like this...", dto.getAnswer());
        assertTrue(dto.isAnswered());
        assertEquals("expert-1", dto.getAnsweredBy());
    }

    @Test
    void questionDto_CreateRequest_SettersWork() {
        QuestionDto.CreateRequest req = new QuestionDto.CreateRequest();
        req.setQuestion("What is Spring Boot?");

        assertEquals("What is Spring Boot?", req.getQuestion());
    }

    @Test
    void questionDto_AnswerRequest_SettersWork() {
        QuestionDto.AnswerRequest req = new QuestionDto.AnswerRequest();
        req.setAnswer("Spring Boot is a framework...");

        assertEquals("Spring Boot is a framework...", req.getAnswer());
    }

    // ==================== ReactionDto ====================

    @Test
    void reactionDto_Builder_SetsAllFields() {
        ReactionDto dto = ReactionDto.builder()
                .postId("post-1")
                .userId("user-1")
                .type("like")
                .count(42L)
                .userReacted(true)
                .build();

        assertEquals("post-1", dto.getPostId());
        assertEquals("user-1", dto.getUserId());
        assertEquals("like", dto.getType());
        assertEquals(42L, dto.getCount());
        assertTrue(dto.getUserReacted());
    }

    @Test
    void reactionDto_NoArgsConstructor_CreatesEmptyObject() {
        ReactionDto dto = new ReactionDto();
        assertNull(dto.getPostId());
        assertNull(dto.getType());
        assertNull(dto.getCount());
        assertNull(dto.getUserReacted());
    }

    @Test
    void reactionDto_AllArgsConstructor_SetsAllFields() {
        ReactionDto dto = new ReactionDto("post-2", "user-2", "heart", 10L, false);

        assertEquals("post-2", dto.getPostId());
        assertEquals("user-2", dto.getUserId());
        assertEquals("heart", dto.getType());
        assertEquals(10L, dto.getCount());
        assertFalse(dto.getUserReacted());
    }
}
