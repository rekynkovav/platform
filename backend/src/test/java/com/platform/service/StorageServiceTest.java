package com.platform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceTest {

    // ==================== LocalStorageService ====================

    private LocalStorageService localStorageService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        localStorageService = new LocalStorageService();
        ReflectionTestUtils.setField(localStorageService, "storagePath", tempDir.toString());
    }

    @Test
    void localStorage_Upload_CreatesFileAndReturnsPath() {
        byte[] data = "test content".getBytes();
        String result = localStorageService.upload(data, "test/file.txt", "text/plain");

        assertNotNull(result);
        assertTrue(result.contains("file.txt"));
    }

    @Test
    void localStorage_Download_ReturnsUploadedContent() {
        byte[] data = "hello world".getBytes();
        localStorageService.upload(data, "download/test.txt", "text/plain");

        byte[] downloaded = localStorageService.download("download/test.txt");
        assertArrayEquals(data, downloaded);
    }

    @Test
    void localStorage_Download_NonExistent_ThrowsException() {
        assertThrows(RuntimeException.class, () ->
                localStorageService.download("nonexistent/file.txt"));
    }

    @Test
    void localStorage_Delete_RemovesFile() {
        byte[] data = "to delete".getBytes();
        localStorageService.upload(data, "delete/me.txt", "text/plain");

        boolean deleted = localStorageService.delete("delete/me.txt");
        assertTrue(deleted);
        assertFalse(localStorageService.exists("delete/me.txt"));
    }

    @Test
    void localStorage_Delete_NonExistent_ReturnsFalse() {
        boolean deleted = localStorageService.delete("nonexistent/file.txt");
        assertFalse(deleted);
    }

    @Test
    void localStorage_Exists_TrueForExistingFile() {
        byte[] data = "exists".getBytes();
        localStorageService.upload(data, "check/exists.txt", "text/plain");

        assertTrue(localStorageService.exists("check/exists.txt"));
    }

    @Test
    void localStorage_Exists_FalseForMissingFile() {
        assertFalse(localStorageService.exists("missing/file.txt"));
    }

    @Test
    void localStorage_GeneratePresignedUrl_ReturnsLocalPath() {
        String url = localStorageService.generatePresignedUrl("some/file.txt", 300);

        assertNotNull(url);
        assertTrue(url.startsWith("/local-storage/"));
        assertTrue(url.contains("expires="));
    }

    @Test
    void localStorage_UploadStream_CreatesFile() {
        byte[] data = "stream content".getBytes();
        InputStream is = new ByteArrayInputStream(data);

        String result = localStorageService.upload(is, "stream/file.txt", "text/plain", data.length);

        assertNotNull(result);
        byte[] downloaded = localStorageService.download("stream/file.txt");
        assertArrayEquals(data, downloaded);
    }

    @Test
    void localStorage_Upload_CreatesParentDirectories() {
        byte[] data = "nested".getBytes();
        String result = localStorageService.upload(data, "a/b/c/deep.txt", "text/plain");

        assertNotNull(result);
        assertTrue(localStorageService.exists("a/b/c/deep.txt"));
    }
}
