package com.platform.service;

import com.platform.exception.ResourceNotFoundException;
import com.platform.model.BroadcastStatus;
import com.platform.model.LiveBroadcast;
import com.platform.repository.LiveBroadcastRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BroadcastArchiveServiceTest {

    @Mock private LiveBroadcastRepository broadcastRepository;
    @Mock private StorageService storageService;

    @InjectMocks
    private BroadcastArchiveService broadcastArchiveService;

    private UUID broadcastId;
    private LiveBroadcast broadcast;

    @BeforeEach
    void setUp() {
        broadcastId = UUID.randomUUID();
        broadcast = LiveBroadcast.builder()
                .id(broadcastId)
                .title("Test Stream")
                .status(BroadcastStatus.ENDED)
                .rtmpStreamKey(broadcastId.toString())
                .ownerId(UUID.randomUUID())
                .endedAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    // ==================== archiveBroadcast ====================

    @Test
    void archiveBroadcast_NullId_ThrowsIllegalArgument() {
        assertThatThrownBy(() -> broadcastArchiveService.archiveBroadcast(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void archiveBroadcast_BlankId_ThrowsIllegalArgument() {
        assertThatThrownBy(() -> broadcastArchiveService.archiveBroadcast("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void archiveBroadcast_NotFound_ThrowsResourceNotFound() {
        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> broadcastArchiveService.archiveBroadcast(broadcastId.toString()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void archiveBroadcast_AlreadyArchived_SkipsConversion() {
        broadcast.setRecordedUrl("archive/abc/recording.mp4");
        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(broadcast));
        when(storageService.isValidMediaFile("archive/abc/recording.mp4")).thenReturn(true);

        broadcastArchiveService.archiveBroadcast(broadcastId.toString());

        verify(broadcastRepository, never()).save(any());
    }

    @Test
    void archiveBroadcast_AlreadyArchived_InvalidS3_KeepsHls() {
        broadcast.setRecordedUrl("archive/abc/recording.mp4");
        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(broadcast));
        when(storageService.isValidMediaFile("archive/abc/recording.mp4")).thenReturn(false);

        broadcastArchiveService.archiveBroadcast(broadcastId.toString());

        verify(broadcastRepository, never()).save(any());
    }

    @Test
    void archiveBroadcast_LiveBroadcast_ThrowsIllegalState() {
        broadcast.setStatus(BroadcastStatus.LIVE);
        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(broadcast));

        assertThatThrownBy(() -> broadcastArchiveService.archiveBroadcast(broadcastId.toString()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot archive live broadcast");
    }

    @Test
    void archiveBroadcast_IdleBroadcast_ThrowsIllegalState() {
        broadcast.setStatus(BroadcastStatus.IDLE);
        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(broadcast));

        assertThatThrownBy(() -> broadcastArchiveService.archiveBroadcast(broadcastId.toString()))
                .isInstanceOf(IllegalStateException.class);
    }

    // ==================== getArchiveStreamUrl ====================

    @Test
    void getArchiveStreamUrl_HasRecording_ReturnsPresignedUrl() {
        broadcast.setRecordedUrl("archive/test/recording.mp4");
        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(broadcast));
        when(storageService.generatePresignedUrl("archive/test/recording.mp4", 3600))
                .thenReturn("https://s3.example.com/presigned");

        String url = broadcastArchiveService.getArchiveStreamUrl(broadcastId.toString(), "user-1");

        assertThat(url).isEqualTo("https://s3.example.com/presigned");
    }

    @Test
    void getArchiveStreamUrl_NoRecording_ThrowsException() {
        broadcast.setRecordedUrl(null);
        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(broadcast));

        assertThatThrownBy(() -> broadcastArchiveService.getArchiveStreamUrl(broadcastId.toString(), "user-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Archive not available");
    }

    @Test
    void getArchiveStreamUrl_BroadcastNotFound_ThrowsException() {
        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> broadcastArchiveService.getArchiveStreamUrl(broadcastId.toString(), "user-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== cleanupStaleHlsSegments ====================

    @Test
    void cleanupStaleHlsSegments_ArchivedBroadcast_CleansUp() {
        broadcast.setRecordedUrl("archive/done/recording.mp4");
        when(broadcastRepository.findByStatus(BroadcastStatus.ENDED)).thenReturn(List.of(broadcast));

        broadcastArchiveService.cleanupStaleHlsSegments();

        // Should not try to archive again, just cleanup
        verify(broadcastRepository, never()).save(any());
    }

    @Test
    void cleanupStaleHlsSegments_RecentBroadcast_Skips() {
        broadcast.setEndedAt(LocalDateTime.now().minusMinutes(30)); // less than 2 hours
        when(broadcastRepository.findByStatus(BroadcastStatus.ENDED)).thenReturn(List.of(broadcast));

        broadcastArchiveService.cleanupStaleHlsSegments();

        verify(broadcastRepository, never()).save(any());
    }

    @Test
    void cleanupStaleHlsSegments_NoEndedBroadcasts_DoesNothing() {
        when(broadcastRepository.findByStatus(BroadcastStatus.ENDED)).thenReturn(List.of());

        broadcastArchiveService.cleanupStaleHlsSegments();

        verify(broadcastRepository, never()).save(any());
    }
}
