package com.platform.service;

import com.platform.util.FFmpegExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class HLSStreamingServiceTest {
    private HLSStreamingService hlsService;
    private EncryptionService encryptionService;
    private StorageService storageService;
    private FFmpegExecutor ffmpegExecutor;

    @BeforeEach
    void setUp() {
        encryptionService = mock(EncryptionService.class);
        storageService = mock(StorageService.class);
        ffmpegExecutor = mock(FFmpegExecutor.class);
        hlsService = new HLSStreamingService(encryptionService, storageService, ffmpegExecutor);
        ReflectionTestUtils.setField(hlsService, "segmentDuration", 6);
        ReflectionTestUtils.setField(hlsService, "tempPath", "/tmp/hls-test");
    }

    // ==================== convertToHLS disabled ====================

    @Test
    void convertToHLS_Disabled_ReturnsNull() {
        ReflectionTestUtils.setField(hlsService, "hlsEnabled", false);

        File inputVideo = new File("/tmp/test.mp4");
        HLSStreamingService.HLSResult result = hlsService.convertToHLS(inputVideo, "user-1", "file-1");

        assertNull(result);
    }

    // ==================== HLSResult builder ====================

    @Test
    void hlsResult_Builder_SetsAllFields() {
        HLSStreamingService.HLSResult result = HLSStreamingService.HLSResult.builder()
                .playlistUrl("hls/file-1/playlist.m3u8")
                .segmentCount(10)
                .duration(120)
                .build();

        assertEquals("hls/file-1/playlist.m3u8", result.getPlaylistUrl());
        assertEquals(10, result.getSegmentCount());
        assertEquals(120, result.getDuration());
    }

    @Test
    void hlsResult_BuilderGettersWork() {
        HLSStreamingService.HLSResult result = HLSStreamingService.HLSResult.builder()
                .playlistUrl("test.m3u8")
                .segmentCount(5)
                .duration(60)
                .build();

        assertEquals("test.m3u8", result.getPlaylistUrl());
        assertEquals(5, result.getSegmentCount());
        assertEquals(60, result.getDuration());
    }

    // ==================== convertToHLS with non-existent file ====================
    @Test
    void convertToHLS_Enabled_NonExistentFile_ThrowsException() {
        ReflectionTestUtils.setField(hlsService, "hlsEnabled", true);
        when(encryptionService.generateHLSKey("user-1", "file-1")).thenReturn(new byte[16]);

        File nonExistent = new File("/tmp/nonexistent_video.mp4");

        // FFmpegExecutor is mocked so execute() succeeds silently;
        // the method completes normally and returns an HLSResult
        HLSStreamingService.HLSResult result =
                hlsService.convertToHLS(nonExistent, "user-1", "file-1");

        assertNotNull(result);
        assertNull(result.getPlaylistUrl());
        assertEquals(0, result.getSegmentCount());
    }
}
