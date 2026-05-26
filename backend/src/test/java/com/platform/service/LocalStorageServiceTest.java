package com.platform.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceTest {

    private LocalStorageService storageService;
    private String testPath;

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageService();
        testPath = "./test-uploads";
        ReflectionTestUtils.setField(storageService, "storagePath", testPath);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up test files
        Path path = Paths.get(testPath);
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                });
        }
    }

    @Test
    void testUploadAndDownload() {
        byte[] data = "Test data".getBytes();
        String path = "test/file.txt";

        String result = storageService.upload(data, path, "text/plain");
        assertNotNull(result);

        byte[] downloaded = storageService.download(path);
        assertArrayEquals(data, downloaded);
    }

    @Test
    void testExists() {
        byte[] data = "Test data".getBytes();
        String path = "test/exists.txt";

        assertFalse(storageService.exists(path));

        storageService.upload(data, path, "text/plain");
        assertTrue(storageService.exists(path));
    }

    @Test
    void testDelete() {
        byte[] data = "Test data".getBytes();
        String path = "test/delete.txt";

        storageService.upload(data, path, "text/plain");
        assertTrue(storageService.exists(path));

        boolean deleted = storageService.delete(path);
        assertTrue(deleted);
        assertFalse(storageService.exists(path));
    }

    @Test
    void testGeneratePresignedUrl() {
        String url = storageService.generatePresignedUrl("test/path.txt", 300);
        assertNotNull(url);
        assertTrue(url.contains("test/path.txt"));
        assertTrue(url.contains("expires="));
    }

    @Test
    void testDownloadNonExistent() {
        assertThrows(RuntimeException.class, () -> {
            storageService.download("non-existent.txt");
        });
    }
}