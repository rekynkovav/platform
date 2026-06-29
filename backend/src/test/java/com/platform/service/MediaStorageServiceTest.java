package com.platform.service;

import com.platform.model.dto.PostDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MediaStorageServiceTest {

    private MediaStorageService mediaStorageService;
    private StorageService storageService;

    @BeforeEach
    void setUp() throws Exception {
        storageService = mock(StorageService.class);
        mediaStorageService = new MediaStorageService(storageService);

        // Set @Value field via reflection
        java.lang.reflect.Field maxField = MediaStorageService.class.getDeclaredField("maxFileSize");
        maxField.setAccessible(true);
        maxField.set(mediaStorageService, 100 * 1024 * 1024L); // 100MB
    }

    // ==================== saveMedia - image ====================

    @Test
    void saveMedia_ImageFile_ReturnsCorrectResponse() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-image-data".getBytes());

        when(storageService.upload(any(), anyString(), anyString(), anyLong()))
                .thenReturn("image/uuid_photo.jpg");
        when(storageService.generatePresignedUrl(anyString(), anyInt()))
                .thenReturn("https://storage.example.com/image/uuid_photo.jpg?sig=abc");

        PostDto.MediaUploadResponse response = mediaStorageService.saveMedia(file, null);

        assertNotNull(response);
        assertEquals("image", response.getType());
        assertEquals(file.getSize(), response.getSize());
        assertTrue(response.getFilename().contains("photo.jpg"));
        assertTrue(response.getUrl().contains("storage.example.com"));
    }

    // ==================== saveMedia - video ====================

    @Test
    void saveMedia_VideoFile_DetectsVideoType() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "clip.mp4", "video/mp4", "fake-video-data".getBytes());

        when(storageService.upload(any(), anyString(), anyString(), anyLong()))
                .thenReturn("video/uuid_clip.mp4");
        when(storageService.generatePresignedUrl(anyString(), anyInt()))
                .thenReturn("https://storage.example.com/video/uuid_clip.mp4?sig=xyz");

        PostDto.MediaUploadResponse response = mediaStorageService.saveMedia(file, null);

        assertEquals("video", response.getType());
    }

    // ==================== saveMedia - explicit type override ====================

    @Test
    void saveMedia_ExplicitType_OverridesDetection() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "pdf-content".getBytes());

        when(storageService.upload(any(), anyString(), anyString(), anyLong()))
                .thenReturn("custom/doc.pdf");
        when(storageService.generatePresignedUrl(anyString(), anyInt()))
                .thenReturn("https://storage.example.com/custom/doc.pdf");

        PostDto.MediaUploadResponse response = mediaStorageService.saveMedia(file, "custom");

        assertEquals("custom", response.getType());
    }

    // ==================== saveMedia - unknown type ====================

    @Test
    void saveMedia_UnknownContentType_ReturnsOther() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "data.bin", "application/octet-stream", "binary".getBytes());

        when(storageService.upload(any(), anyString(), anyString(), anyLong()))
                .thenReturn("other/data.bin");
        when(storageService.generatePresignedUrl(anyString(), anyInt()))
                .thenReturn("https://storage.example.com/other/data.bin");

        PostDto.MediaUploadResponse response = mediaStorageService.saveMedia(file, null);

        assertEquals("other", response.getType());
    }

    // ==================== saveMedia - IO exception ====================

    @Test
    void saveMedia_UploadFails_ThrowsRuntimeException() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fail.jpg", "image/jpeg", "data".getBytes());

        when(storageService.upload(any(), anyString(), anyString(), anyLong()))
                .thenThrow(new RuntimeException("S3 unavailable"));

        assertThrows(RuntimeException.class, () ->
                mediaStorageService.saveMedia(file, null));
    }
}
