package com.platform.model.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventMaterialQuestionDtoTest {

    // ==================== EventMaterialDto ====================

    @Test
    void eventMaterialDto_Builder_SetsAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        EventMaterialDto dto = EventMaterialDto.builder()
                .id(id)
                .sectionId("about")
                .type("text")
                .title("About Section")
                .content("Detailed description")
                .url(null)
                .mediaId(null)
                .createdBy("admin-1")
                .authorName("Admin")
                .sortOrder(1)
                .isActive(true)
                .createdAt(now)
                .updatedAt(now.plusMinutes(5))
                .build();

        assertEquals(id, dto.getId());
        assertEquals("about", dto.getSectionId());
        assertEquals("text", dto.getType());
        assertEquals("About Section", dto.getTitle());
        assertEquals("Detailed description", dto.getContent());
        assertEquals(1, dto.getSortOrder());
        assertTrue(dto.getIsActive());
    }

    @Test
    void eventMaterialDto_CreateRequest_SettersWork() {
        EventMaterialDto.CreateRequest req = new EventMaterialDto.CreateRequest();
        req.setSectionId("interviews");
        req.setType("video");
        req.setTitle("Interview with Expert");
        req.setContent("Full transcript");
        req.setMediaId("media-123");
        req.setSortOrder(2);

        assertEquals("interviews", req.getSectionId());
        assertEquals("video", req.getType());
        assertEquals("Interview with Expert", req.getTitle());
        assertEquals("media-123", req.getMediaId());
        assertEquals(2, req.getSortOrder());
    }

    @Test
    void eventMaterialDto_UpdateRequest_PartialUpdate_Works() {
        EventMaterialDto.UpdateRequest req = new EventMaterialDto.UpdateRequest();
        req.setTitle("Updated Title");
        req.setIsActive(false);

        assertEquals("Updated Title", req.getTitle());
        assertFalse(req.getIsActive());
        assertNull(req.getContent());
        assertNull(req.getUrl());
    }

    // ==================== QuestionDto ====================

    @Test
    void questionDto_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();

        QuestionDto dto = QuestionDto.builder()
                .id("q-1")
                .question("What is the best diet?")
                .answer("Balanced nutrition with whole foods.")
                .authorId("user-1")
                .authorName("Alice")
                .mode("public")
                .recipientId("expert-1")
                .isAnswered(true)
                .createdAt(now.minusHours(2))
                .answeredAt(now)
                .answeredBy("expert-1")
                .build();

        assertEquals("q-1", dto.getId());
        assertEquals("What is the best diet?", dto.getQuestion());
        assertEquals("Balanced nutrition with whole foods.", dto.getAnswer());
        assertTrue(dto.isAnswered());
        assertEquals("expert-1", dto.getAnsweredBy());
    }

    @Test
    void questionDto_CreateRequest_SettersWork() {
        QuestionDto.CreateRequest req = new QuestionDto.CreateRequest();
        req.setQuestion("How to lose weight safely?");

        assertEquals("How to lose weight safely?", req.getQuestion());
    }

    @Test
    void questionDto_AnswerRequest_SettersWork() {
        QuestionDto.AnswerRequest req = new QuestionDto.AnswerRequest();
        req.setAnswer("Consult a nutritionist and maintain caloric deficit.");

        assertEquals("Consult a nutritionist and maintain caloric deficit.", req.getAnswer());
    }
}
