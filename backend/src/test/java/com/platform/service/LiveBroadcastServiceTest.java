package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.LiveBroadcast;
import com.platform.model.dto.LiveBroadcastDto;
import com.platform.model.enums.BroadcastStatus;
import com.platform.repository.LiveBroadcastRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveBroadcastServiceTest {

    @Mock
    private LiveBroadcastRepository broadcastRepository;

    @Mock
    private BroadcastArchiveService archiveService;

    @InjectMocks
    private LiveBroadcastService liveBroadcastService;

    private String userId;
    private UUID userIdUuid;
    private String broadcastId;
    private UUID broadcastIdUuid;
    private String ownerId;
    private UUID ownerIdUuid;

    @BeforeEach
    void setUp() {
        userId = "user-123";
        userIdUuid = UUID.fromString(userId);
        broadcastId = "broadcast-456";
        broadcastIdUuid = UUID.fromString(broadcastId);
        ownerId = "owner-789";
        ownerIdUuid = UUID.fromString(ownerId);
    }

    @Test
    void createBroadcast_CreatesWithIdleStatusAndStreamKey() {
        // Arrange
        String title = "Test Broadcast";
        String description = "Test description";

        when(broadcastRepository.save(any(LiveBroadcast.class))).thenAnswer(inv -> {
            LiveBroadcast broadcast = inv.getArgument(0);
            broadcast.setId(broadcastIdUuid);
            return broadcast;
        });

        // Act
        LiveBroadcastDto result = liveBroadcastService.createBroadcast(title, description, userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(result.getOwnerId()).isEqualTo(userIdUuid);
        assertThat(result.getStatus()).isEqualTo(BroadcastStatus.IDLE);
        assertThat(result.getRtmpStreamKey()).isEqualTo(broadcastId);
        verify(broadcastRepository).save(any(LiveBroadcast.class));
    }

    @Test
    void startBroadcast_ByOwner_StartsSuccessfully() {
        // Arrange
        LiveBroadcast broadcast = LiveBroadcast.builder()
                .id(broadcastIdUuid)
                .ownerId(ownerIdUuid)
                .title("Test Broadcast")
                .status(BroadcastStatus.IDLE)
                .build();

        when(broadcastRepository.findById(broadcastIdUuid)).thenReturn(Optional.of(broadcast));
        when(broadcastRepository.save(any(LiveBroadcast.class))).thenReturn(broadcast);

        // Act
        String rtmpUrl = liveBroadcastService.startBroadcast(broadcastId, ownerId);

        // Assert
        assertThat(rtmpUrl).isNotNull();
        assertThat(rtmpUrl).contains("rtmp://");
        assertThat(rtmpUrl).contains(broadcastId);
        assertThat(broadcast.getStatus()).isEqualTo(BroadcastStatus.LIVE);
        assertThat(broadcast.getStartedAt()).isNotNull();
        verify(broadcastRepository).save(broadcast);
    }

    @Test
    void startBroadcast_ByOtherUser_ThrowsException() {
        // Arrange
        String otherUserId = "other-999";
        LiveBroadcast broadcast = LiveBroadcast.builder()
                .id(broadcastIdUuid)
                .ownerId(ownerIdUuid)
                .title("Test Broadcast")
                .status(BroadcastStatus.IDLE)
                .build();

        when(broadcastRepository.findById(broadcastIdUuid)).thenReturn(Optional.of(broadcast));

        // Act & Assert
        assertThatThrownBy(() -> liveBroadcastService.startBroadcast(broadcastId, otherUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permission");

        verify(broadcastRepository, never()).save(any(LiveBroadcast.class));
    }

    @Test
    void stopBroadcast_ByOwner_StopsSuccessfully() {
        // Arrange
        LiveBroadcast broadcast = LiveBroadcast.builder()
                .id(broadcastIdUuid)
                .ownerId(ownerIdUuid)
                .title("Test Broadcast")
                .status(BroadcastStatus.LIVE)
                .startedAt(Instant.now().minusSeconds(300))
                .build();

        when(broadcastRepository.findById(broadcastIdUuid)).thenReturn(Optional.of(broadcast));
        when(broadcastRepository.save(any(LiveBroadcast.class))).thenReturn(broadcast);

        // Act
        liveBroadcastService.stopBroadcast(broadcastId, ownerId);

        // Assert
        assertThat(broadcast.getStatus()).isEqualTo(BroadcastStatus.ENDED);
        assertThat(broadcast.getEndedAt()).isNotNull();
        verify(archiveService).archiveBroadcast(broadcastId);
        verify(broadcastRepository).save(broadcast);
    }

    @Test
    void stopBroadcast_ByOtherUser_ThrowsException() {
        // Arrange
        String otherUserId = "other-999";
        LiveBroadcast broadcast = LiveBroadcast.builder()
                .id(broadcastIdUuid)
                .ownerId(ownerIdUuid)
                .title("Test Broadcast")
                .status(BroadcastStatus.LIVE)
                .build();

        when(broadcastRepository.findById(broadcastIdUuid)).thenReturn(Optional.of(broadcast));

        // Act & Assert
        assertThatThrownBy(() -> liveBroadcastService.stopBroadcast(broadcastId, otherUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permission");

        verify(broadcastRepository, never()).save(any(LiveBroadcast.class));
        verify(archiveService, never()).archiveBroadcast(any());
    }

    @Test
    void getBroadcast_Found_ReturnsBroadcast() {
        // Arrange
        LiveBroadcast broadcast = LiveBroadcast.builder()
                .id(broadcastIdUuid)
                .ownerId(ownerIdUuid)
                .title("Test Broadcast")
                .status(BroadcastStatus.LIVE)
                .viewerCount(100)
                .maxViewers(150)
                .build();

        when(broadcastRepository.findById(broadcastIdUuid)).thenReturn(Optional.of(broadcast));

        // Act
        LiveBroadcastDto result = liveBroadcastService.getBroadcast(broadcastId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(broadcastIdUuid);
        assertThat(result.getTitle()).isEqualTo("Test Broadcast");
        assertThat(result.getStatus()).isEqualTo(BroadcastStatus.LIVE);
        assertThat(result.getViewerCount()).isEqualTo(100);
        assertThat(result.getMaxViewers()).isEqualTo(150);
    }

    @Test
    void getBroadcast_NotFound_ThrowsException() {
        // Arrange
        String nonExistentBroadcastId = "non-existent-broadcast";
        UUID nonExistentUuid = UUID.fromString(nonExistentBroadcastId);
        when(broadcastRepository.findById(nonExistentUuid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> liveBroadcastService.getBroadcast(nonExistentBroadcastId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Broadcast not found");
    }

    @Test
    void getHlsPlaylist_LiveBroadcast_ReturnsUrl() {
        // Arrange
        LiveBroadcast broadcast = LiveBroadcast.builder()
                .id(broadcastIdUuid)
                .ownerId(ownerIdUuid)
                .title("Test Broadcast")
                .status(BroadcastStatus.LIVE)
                .build();

        when(broadcastRepository.findById(broadcastIdUuid)).thenReturn(Optional.of(broadcast));

        // Act
        String hlsUrl = liveBroadcastService.getHlsPlaylist(broadcastId);

        // Assert
        assertThat(hlsUrl).isNotNull();
        assertThat(hlsUrl).contains("http://");
        assertThat(hlsUrl).contains(broadcastId);
        assertThat(hlsUrl).contains(".m3u8");
    }

    @Test
    void getHlsPlaylist_NotLive_ThrowsException() {
        // Arrange
        LiveBroadcast broadcast = LiveBroadcast.builder()
                .id(broadcastIdUuid)
                .ownerId(ownerIdUuid)
                .title("Test Broadcast")
                .status(BroadcastStatus.IDLE)  // Not LIVE
                .build();

        when(broadcastRepository.findById(broadcastIdUuid)).thenReturn(Optional.of(broadcast));

        // Act & Assert
        assertThatThrownBy(() -> liveBroadcastService.getHlsPlaylist(broadcastId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not live");
    }

    @Test
    void onStreamStart_UpdatesStatusToLive() {
        // Arrange
        String streamKey = broadcastId;
        LiveBroadcast broadcast = LiveBroadcast.builder()
                .id(broadcastIdUuid)
                .ownerId(ownerIdUuid)
                .title("Test Broadcast")
                .status(BroadcastStatus.IDLE)
                .build();

        when(broadcastRepository.findByRtmpStreamKey(streamKey)).thenReturn(Optional.of(broadcast));
        when(broadcastRepository.save(any(LiveBroadcast.class))).thenReturn(broadcast);

        // Act
        liveBroadcastService.onStreamStart(streamKey);

        // Assert
        assertThat(broadcast.getStatus()).isEqualTo(BroadcastStatus.LIVE);
        assertThat(broadcast.getStartedAt()).isNotNull();
        verify(broadcastRepository).save(broadcast);
    }

    @Test
    void addViewer_IncrementsViewerCount() {
        // Arrange
        LiveBroadcast broadcast = LiveBroadcast.builder()
                .id(broadcastIdUuid)
                .ownerId(ownerIdUuid)
                .title("Test Broadcast")
                .status(BroadcastStatus.LIVE)
                .viewerCount(50)
                .maxViewers(100)
                .build();

        when(broadcastRepository.findById(broadcastIdUuid)).thenReturn(Optional.of(broadcast));
        when(broadcastRepository.save(any(LiveBroadcast.class))).thenReturn(broadcast);

        // Act
        liveBroadcastService.addViewer(broadcastId);

        // Assert
        assertThat(broadcast.getViewerCount()).isEqualTo(51);
        verify(broadcastRepository).save(broadcast);
    }

    @Test
    void addViewer_SetsNewMaxViewersRecord() {
        // Arrange
        LiveBroadcast broadcast = LiveBroadcast.builder()
                .id(broadcastIdUuid)
                .ownerId(ownerIdUuid)
                .title("Test Broadcast")
                .status(BroadcastStatus.LIVE)
                .viewerCount(150)  // Current
                .maxViewers(100)   // Old record
                .build();

        when(broadcastRepository.findById(broadcastIdUuid)).thenReturn(Optional.of(broadcast));
        when(broadcastRepository.save(any(LiveBroadcast.class))).thenReturn(broadcast);

        // Act
        liveBroadcastService.addViewer(broadcastId);

        // Assert
        assertThat(broadcast.getViewerCount()).isEqualTo(151);
        assertThat(broadcast.getMaxViewers()).isEqualTo(151);  // New record
        verify(broadcastRepository).save(broadcast);
    }

    @Test
    void deleteBroadcast_ByOtherUser_ThrowsException() {
        // Arrange
        String otherUserId = "other-999";
        LiveBroadcast broadcast = LiveBroadcast.builder()
                .id(broadcastIdUuid)
                .ownerId(ownerIdUuid)
                .title("Test Broadcast")
                .status(BroadcastStatus.ENDED)
                .build();

        when(broadcastRepository.findById(broadcastIdUuid)).thenReturn(Optional.of(broadcast));

        // Act & Assert
        assertThatThrownBy(() -> liveBroadcastService.deleteBroadcast(broadcastId, otherUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permission");

        verify(broadcastRepository, never()).delete(any());
    }
}