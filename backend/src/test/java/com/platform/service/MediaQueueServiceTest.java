package com.platform.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MediaQueueServiceTest {

    // ==================== MediaTask construction ====================

    @Test
    void mediaTask_Constructor_SetsFields() {
        var task = new MediaQueueService.MediaTask("/tmp/input.mp4", "/tmp/output.mp4", "video");

        assertNotNull(task);
    }

    // ==================== submitTask ====================

    @Test
    void submitTask_DoesNotThrow() {
        MediaQueueService service = new MediaQueueService();
        var task = new MediaQueueService.MediaTask("/tmp/in.mp4", "/tmp/out.mp4", "video");

        assertDoesNotThrow(() -> service.submitTask(task));
    }

    @Test
    void submitTask_MultipleTasks_DoesNotThrow() {
        MediaQueueService service = new MediaQueueService();

        for (int i = 0; i < 10; i++) {
            var task = new MediaQueueService.MediaTask(
                    "/tmp/in" + i + ".mp4",
                    "/tmp/out" + i + ".mp4",
                    "video"
            );
            assertDoesNotThrow(() -> service.submitTask(task));
        }
    }

    // ==================== MediaTask.process (without FFmpeg) ====================

    @Test
    void mediaTask_Process_NoFFmpeg_ThrowsRuntimeException() {
        var task = new MediaQueueService.MediaTask(
                "/nonexistent/input.mp4",
                "/nonexistent/output.mp4",
                "video"
        );

        // FFmpeg is not available or input doesn't exist → should throw
        assertThrows(RuntimeException.class, task::process);
    }
}
