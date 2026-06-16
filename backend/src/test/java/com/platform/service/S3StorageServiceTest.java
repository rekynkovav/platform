package com.platform.service;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3StorageServiceTest {

    private S3StorageService s3StorageService;
    private AmazonS3 mockS3Client;

    @BeforeEach
    void setUp() {
        s3StorageService = new S3StorageService();
        ReflectionTestUtils.setField(s3StorageService, "accessKey", "test-access-key");
        ReflectionTestUtils.setField(s3StorageService, "secretKey", "test-secret-key");
        ReflectionTestUtils.setField(s3StorageService, "bucket", "test-bucket");
        ReflectionTestUtils.setField(s3StorageService, "endpoint", "http://localhost:19999");
        ReflectionTestUtils.setField(s3StorageService, "region", "ru-central1");

        // Inject a mock S3 client that throws on all calls
        mockS3Client = mock(AmazonS3.class, invocation -> {
            throw new SdkClientException("No S3 connection");
        });
        ReflectionTestUtils.setField(s3StorageService, "s3Client", mockS3Client);
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
        assertThrows(Exception.class, () ->
                s3StorageService.generatePresignedUrl("file.txt", 3600));
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
