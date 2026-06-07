package com.platform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceTest {

    private LocalStorageService localStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        localStorageService = new LocalStorageService();
        ReflectionTestUtils.setField(localStorageService, "storagePath", tempDir.toString());
    }

    // ==================== upload (byte[]) ====================

    @Test
    void upload_ByteArray_CreatesFile() {
        byte[] data = "test content".getBytes();
        String path = "uploads/test.txt";

        String result = localStorageService.upload(data, path, "text/plain");

        assertNotNull(result);
        assertTrue(Files.exists(tempDir.resolve(path)));
    }

    @Test
    void upload_ByteArray_CreatesParentDirectories() {
        byte[] data = "nested".getBytes();
        String path = "a/b/c/file.txt";

        localStorageService.upload(data, path, "text/plain");

        assertTrue(Files.exists(tempDir.resolve(path)));
    }

    // ==================== download ====================

    @Test
    void download_ExistingFile_ReturnsContent() throws IOException {
        String path = "download-test.txt";
        byte[] expected = "hello world".getBytes();
        Files.write(tempDir.resolve(path), expected);

        byte[] result = localStorageService.download(path);

        assertArrayEquals(expected, result);
    }

    @Test
    void download_NonExistentFile_ThrowsException() {
        assertThrows(RuntimeException.class, () ->
                localStorageService.download("nonexistent.txt"));
    }

    // ==================== delete ====================

    @Test
    void delete_ExistingFile_ReturnsTrue() throws IOException {
        String path = "to-delete.txt";
        Files.write(tempDir.resolve(path), "data".getBytes());

        assertTrue(localStorageService.delete(path));
        assertFalse(Files.exists(tempDir.resolve(path)));
    }

    @Test
    void delete_NonExistentFile_ReturnsFalse() {
        assertFalse(localStorageService.delete("no-such-file.txt"));
    }

    // ==================== exists ====================

    @Test
    void exists_ExistingFile_ReturnsTrue() throws IOException {
        String path = "exists.txt";
        Files.write(tempDir.resolve(path), "data".getBytes());

        assertTrue(localStorageService.exists(path));
    }

    @Test
    void exists_NonExistentFile_ReturnsFalse() {
        assertFalse(localStorageService.exists("missing.txt"));
    }

    // ==================== generatePresignedUrl ====================

    @Test
    void generatePresignedUrl_ReturnsLocalPath() {
        String url = localStorageService.generatePresignedUrl("media/video.mp4", 3600);

        assertNotNull(url);
        assertTrue(url.startsWith("/local-storage/media/video.mp4?expires="));
    }

    // ==================== upload (InputStream) ====================

    @Test
    void upload_InputStream_CreatesFile() {
        byte[] data = "stream content".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        String path = "stream-upload.txt";

        String result = localStorageService.upload(inputStream, path, "text/plain", data.length);

        assertNotNull(result);
        assertTrue(Files.exists(tempDir.resolve(path)));
    }
}
