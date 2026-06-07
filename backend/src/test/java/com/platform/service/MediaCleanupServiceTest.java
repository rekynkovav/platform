package com.platform.service;

import com.platform.model.MediaFile;
import com.platform.repository.MediaFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MediaCleanupServiceTest {

    private MediaCleanupService mediaCleanupService;
    private MediaFileRepository mediaFileRepository;
    private StorageService storageService;

    @BeforeEach
    void setUp() {
        mediaFileRepository = mock(MediaFileRepository.class);
        storageService = mock(StorageService.class);
        mediaCleanupService = new MediaCleanupService(mediaFileRepository, storageService);
    }

    // ==================== cleanupOldHlsSegments ====================

    @Test
    void cleanupOldHlsSegments_DeletesSegmentsAndFiles() {
        MediaFile segment1 = createSegment("hls/old/segment1.ts");
        MediaFile segment2 = createSegment("hls/old/segment2.ts");

        when(mediaFileRepository.findOldHlsSegments(any(LocalDateTime.class)))
                .thenReturn(List.of(segment1, segment2));

        mediaCleanupService.cleanupOldHlsSegments();

        verify(storageService).delete("hls/old/segment1.ts");
        verify(storageService).delete("hls/old/segment2.ts");
        verify(mediaFileRepository).delete(segment1);
        verify(mediaFileRepository).delete(segment2);
    }

    @Test
    void cleanupOldHlsSegments_NoOldSegments_DoesNothing() {
        when(mediaFileRepository.findOldHlsSegments(any(LocalDateTime.class)))
                .thenReturn(List.of());

        mediaCleanupService.cleanupOldHlsSegments();

        verifyNoInteractions(storageService);
        verify(mediaFileRepository, never()).delete(any());
    }

    @Test
    void cleanupOldHlsSegments_StorageDeleteFails_ContinuesWithNext() {
        MediaFile segment1 = createSegment("hls/fail.ts");
        MediaFile segment2 = createSegment("hls/ok.ts");

        when(mediaFileRepository.findOldHlsSegments(any(LocalDateTime.class)))
                .thenReturn(List.of(segment1, segment2));
        doThrow(new RuntimeException("S3 error")).when(storageService).delete("hls/fail.ts");

        mediaCleanupService.cleanupOldHlsSegments();

        // segment1 failed but segment2 should still be processed
        verify(storageService).delete("hls/ok.ts");
        verify(mediaFileRepository).delete(segment2);
        // segment1 should NOT be deleted from DB since storage delete failed
        verify(mediaFileRepository, never()).delete(segment1);
    }

    @Test
    void cleanupOldHlsSegments_UsesCorrectCutoff_SevenDaysAgo() {
        when(mediaFileRepository.findOldHlsSegments(any(LocalDateTime.class)))
                .thenReturn(List.of());

        mediaCleanupService.cleanupOldHlsSegments();

        verify(mediaFileRepository).findOldHlsSegments(argThat(cutoff -> {
            LocalDateTime expected = LocalDateTime.now().minusDays(7);
            return Math.abs(java.time.Duration.between(expected, cutoff).toSeconds()) < 60;
        }));
    }

    private MediaFile createSegment(String s3Key) {
        MediaFile mf = new MediaFile();
        mf.setId(UUID.randomUUID().toString());
        mf.setS3Key(s3Key);
        return mf;
    }
}
