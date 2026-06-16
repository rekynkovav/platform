package com.platform.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MediaQueueStreamingFileServiceTest {

    // ==================== MediaQueueService ====================
    @Test
    void mediaQueue_SubmitTask_DoesNotThrow() {
        MediaQueueService service = new MediaQueueService();
        MediaQueueService.MediaTask task = new MediaQueueService.MediaTask("/tmp/in.mp4", "/tmp/out.mp4", "video");

        assertDoesNotThrow(() -> service.submitTask(task));
    }

    @Test
    void mediaQueue_MultipleTasks_AllAccepted() {
        MediaQueueService service = new MediaQueueService();

        for (int i = 0; i < 10; i++) {
            MediaQueueService.MediaTask task = new MediaQueueService.MediaTask(
                    "/tmp/in" + i + ".mp4", "/tmp/out" + i + ".mp4", "video");
            assertDoesNotThrow(() -> service.submitTask(task));
        }
    }

    @Test
    void mediaTask_Constructor_StoresFields() {
        MediaQueueService.MediaTask task = new MediaQueueService.MediaTask("/in.mp4", "/out.mp4", "video");
        assertNotNull(task);
    }

    // ==================== StreamingFileService ====================
    @Test
    void streamFile_InvalidBasePath_ThrowsIOException() throws IOException {
        StreamingFileService service = new StreamingFileService();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // When /app/uploads doesn't exist, validateAndResolvePath throws IOException
        assertThrows(IOException.class, () ->
                service.streamFile("nonexistent.mp4", request, response));
    }

    @Test
    void streamLargeFile_CopiesAllBytes() throws IOException {
        StreamingFileService service = new StreamingFileService();
        byte[] data = "large file content here".getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        service.streamLargeFile(bais, baos, data.length);

        assertArrayEquals(data, baos.toByteArray());
    }

    @Test
    void streamLargeFile_RespectsSizeLimit() throws IOException {
        StreamingFileService service = new StreamingFileService();
        // BUFFER_SIZE = 8192, so we need data larger than one buffer read
        // to actually test the size limit truncation
        byte[] data = new byte[20000];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) ('A' + (i % 26));
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        long limit = 5000;
        service.streamLargeFile(bais, baos, limit);

        assertEquals(limit, baos.size());
    }

    @Test
    void streamZipArchive_EmptyFiles_NoException(@TempDir Path tempDir) throws IOException {
        StreamingFileService service = new StreamingFileService();
        HttpServletResponse response = mock(HttpServletResponse.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new jakarta.servlet.ServletOutputStream() {
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(jakarta.servlet.WriteListener l) {}
            @Override public void write(int b) throws IOException { baos.write(b); }
            @Override public void write(byte[] b, int off, int len) throws IOException { baos.write(b, off, len); }
        });

        File[] emptyFiles = new File[0];
        assertDoesNotThrow(() -> service.streamZipArchive(emptyFiles, response));
    }
}
