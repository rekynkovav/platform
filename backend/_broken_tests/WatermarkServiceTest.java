package com.platform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class WatermarkServiceTest {
    class WatermarkServiceTest {
        private WatermarkService service;
        private FFmpegExecutor ffmpegExecutor;

        @BeforeEach
        void setUp() {
            ffmpegExecutor = mock(FFmpegExecutor.class);
            service = new WatermarkService(ffmpegExecutor);
        ReflectionTestUtils.setField(service, "fontPath", "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf");
        ReflectionTestUtils.setField(service, "tempPath", "/tmp/watermarks");
    }

    // ==================== generateWatermarkData ====================

    @Test
    void generateWatermarkData_ContainsAllFields() {
        String data = service.generateWatermarkData("user-1", "file-1", "video");

        assertNotNull(data);
        assertTrue(data.contains("\"userId\":\"user-1\""));
        assertTrue(data.contains("\"fileId\":\"file-1\""));
        assertTrue(data.contains("\"type\":\"video\""));
        assertTrue(data.contains("\"timestamp\":"));
    }

    @Test
    void generateWatermarkData_DifferentTimestamps() throws InterruptedException {
        String data1 = service.generateWatermarkData("u1", "f1", "image");
        Thread.sleep(5);
        String data2 = service.generateWatermarkData("u1", "f1", "image");

        assertNotEquals(data1, data2);
    }

    @Test
    void generateWatermarkData_ValidJsonFormat() {
        String data = service.generateWatermarkData("u1", "f1", "video");

        assertTrue(data.startsWith("{"));
        assertTrue(data.endsWith("}"));
    }

    // ==================== extractWatermark ====================

    @Test
    void extractWatermark_ReturnsPlaceholder() {
        java.io.File tempFile = new java.io.File("/tmp/test_video.mp4");
        String result = service.extractWatermark(tempFile);

        assertEquals("watermark_data_placeholder", result);
    }

    @Test
    void extractWatermark_NonExistentFile_ReturnsPlaceholder() {
        java.io.File nonExistent = new java.io.File("/tmp/nonexistent_video.mp4");
        String result = service.extractWatermark(nonExistent);

        // Should not throw, returns placeholder or null
        assertTrue(result == null || result.equals("watermark_data_placeholder"));
    }

    // ==================== checkFFmpeg ====================

    @Test
    void checkFFmpeg_DoesNotThrow() {
        // This method calls Runtime.exec which may fail in test env,
        // but should never throw uncaught exception
        assertDoesNotThrow(service::checkFFmpeg);
    }
}
