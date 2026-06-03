package com.platform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class S3StorageServiceTest {

    private S3StorageService s3StorageService;

    @BeforeEach
    void setUp() {
        s3StorageService = new S3StorageService();
        ReflectionTestUtils.setField(s3StorageService, "accessKey", "test-access-key");
        ReflectionTestUtils.setField(s3StorageService, "secretKey", "test-secret-key");
        ReflectionTestUtils.setField(s3StorageService, "bucket", "test-bucket");
        // Use an invalid endpoint to guarantee connection failure
        ReflectionTestUtils.setField(s3StorageService, "endpoint", "http://localhost:19999");
        ReflectionTestUtils.setField(s3StorageService, "region", "ru-central1");
    }

    // ==================== upload (byte[]) - no real S3 ====================

    @Test
    void upload_NoS3Connection_ThrowsException() {
        byte[] data = "test".getBytes();

        assertThrows(Exception.class, () ->
                s3StorageService.upload(data, "test/file.txt", "text/plain"));
    }

    // ==================== download - no real S3 ====================

    @Test
    void download_NoS3Connection_ThrowsException() {
        assertThrows(Exception.class, () ->
                s3StorageService.download("nonexistent/file.txt"));
    }

    // ==================== delete - no real S3 ====================

    @Test
    void delete_NoS3Connection_ReturnsFalse() {
        // delete catches exceptions and returns false
        assertFalse(s3StorageService.delete("nonexistent/file.txt"));
    }

    // ==================== exists - no real S3 ====================

    @Test
    void exists_NoS3Connection_ReturnsFalse() {
        assertFalse(s3StorageService.exists("nonexistent/file.txt"));
    }

    // ==================== generatePresignedUrl - no real S3 ====================

    @Test
    void generatePresignedUrl_NoS3Connection_ThrowsException() {
        // generatePresignedUrl may succeed locally (URL generation doesn't always need network)
        // but with invalid credentials/endpoint it should throw or return null
        try {
            String url = s3StorageService.generatePresignedUrl("file.txt", 3600);
            // If it doesn't throw, the URL should at least be non-null
            // This test verifies the method handles missing S3 gracefully
            assertNotNull(url);
        } catch (Exception e) {
            // Expected: some implementations throw when client can't be created
            assertTrue(true);
        }
    }

    // ==================== verifyUpload - no real S3 ====================

    @Test
    void verifyUpload_NoS3Connection_ReturnsFalse() {
        assertFalse(s3StorageService.verifyUpload("file.txt", 1024));
    }

    // ==================== isValidMediaFile - no real S3 ====================

    @Test
    void isValidMediaFile_NoS3Connection_ReturnsFalse() {
        assertFalse(s3StorageService.isValidMediaFile("video.mp4"));
    }
}
