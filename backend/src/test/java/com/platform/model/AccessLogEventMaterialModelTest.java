package com.platform.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccessLogEventMaterialModelTest {

    // ==================== AccessLog ====================

    @Test
    void accessLog_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();

        AccessLog log = AccessLog.builder()
                .id(1L)
                .fileId("media-1")
                .userId("user-1")
                .action("download")
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0")
                .success(true)
                .errorMessage(null)
                .timestamp(now)
                .build();

        assertEquals(1L, log.getId());
        assertEquals("media-1", log.getFileId());
        assertEquals("user-1", log.getUserId());
        assertEquals("download", log.getAction());
        assertEquals("192.168.1.100", log.getIpAddress());
        assertEquals("Mozilla/5.0", log.getUserAgent());
        assertTrue(log.isSuccess());
        assertNull(log.getErrorMessage());
        assertEquals(now, log.getTimestamp());
    }

    @Test
    void accessLog_OnCreate_SetsTimestamp() {
        AccessLog log = new AccessLog();
        log.setFileId("f1");
        log.setUserId("u1");
        log.setAction("view");

        log.onCreate();

        assertNotNull(log.getTimestamp());
    }

    @Test
    void accessLog_DefaultConstructor_CreatesInstance() {
        AccessLog log = new AccessLog();
        assertNull(log.getId());
        assertFalse(log.isSuccess());
        assertNull(log.getTimestamp());
    }

    // ==================== EventMaterial ====================

    @Test
    void eventMaterial_Builder_SetsAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        EventMaterial material = EventMaterial.builder()
                .id(id)
                .sectionId("interviews")
                .type("video")
                .title("Interview with Expert")
                .content("Full interview transcript")
                .url("https://cdn.example.com/interview.mp4")
                .mediaId("media-int-1")
                .createdBy("admin-1")
                .authorName("Admin")
                .sortOrder(5)
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(id, material.getId());
        assertEquals("interviews", material.getSectionId());
        assertEquals("video", material.getType());
        assertEquals("Interview with Expert", material.getTitle());
        assertEquals("Full interview transcript", material.getContent());
        assertEquals("https://cdn.example.com/interview.mp4", material.getUrl());
        assertEquals("media-int-1", material.getMediaId());
        assertEquals("admin-1", material.getCreatedBy());
        assertEquals("Admin", material.getAuthorName());
        assertEquals(5, material.getSortOrder());
        assertTrue(material.getIsActive());
    }

    @Test
    void eventMaterial_DefaultValues_AreCorrect() {
        EventMaterial material = new EventMaterial();
        assertEquals(0, material.getSortOrder());
        assertTrue(material.getIsActive());
    }

    @Test
    void eventMaterial_SettersAndGettersWork() {
        EventMaterial material = new EventMaterial();
        material.setSectionId("about");
        material.setType("text");
        material.setTitle("About Us");
        material.setSortOrder(1);
        material.setIsActive(false);

        assertEquals("about", material.getSectionId());
        assertEquals("text", material.getType());
        assertEquals("About Us", material.getTitle());
        assertEquals(1, material.getSortOrder());
        assertFalse(material.getIsActive());
    }
}
