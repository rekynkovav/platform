package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.BroadcastStatus;
import com.platform.model.LiveBroadcast;
import com.platform.model.dto.LiveBroadcastDto;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveBroadcastServiceTest {

    @Mock
    private LiveBroadcastRepository broadcastRepository;

    @Mock
    private BroadcastArchiveService archiveService;

    @InjectMocks
    private LiveBroadcastService broadcastService;

    private UUID userId;
    private UUID broadcastId;
    private LiveBroadcast broadcast;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        broadcastId = UUID.randomUUID();

        broadcast = LiveBroadcast.builder()
                .id(broadcastId)
                .title("Test Broadcast")
                .description("Test Description")
                .streamType("live")
                .ownerId(userId)
                .status(BroadcastStatus.IDLE)
                .rtmpStreamKey(broadcastId.toString())
                .viewerCount(0)
                .build();
    }

    @Test
    void createBroadcast_SavesAndReturnsBroadcast() {
        when(broadcastRepository.save(any(LiveBroadcast.class))).thenAnswer(inv -> inv.getArgument(0));

        LiveBroadcast result = broadcastService.createBroadcast(
                "Title", "Description", "live", userId.toString());

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Title");
        assertThat(result.getStatus()).isEqualTo(BroadcastStatus.IDLE);
        assertThat(result.getOwnerId()).isEqualTo(userId);
        verify(broadcastRepository).save(any(LiveBroadcast.class));
    }

    @Test
    void startBroadcast_OwnerCanStart() {
        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(broadcast));
        when(broadcastRepository.save(any(LiveBroadcast.class))).thenAnswer(inv -> inv.getArgument(0));

        String rtmpUrl = broadcastService.startBroadcast(broadcastId.toString(), userId.toString());

        assertThat(rtmpUrl).contains("rtmp://");
        assertThat(broadcast.getStatus()).isEqualTo(BroadcastStatus.LIVE);
        assertThat(broadcast.getStartedAt()).isNotNull();
        verify(broadcastRepository).save(broadcast);
    }

    @Test
    void startBroadcast_NonOwner_ThrowsException() {
        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(broadcast));

        assertThatThrownBy(() -> broadcastService.startBroadcast(
                broadcastId.toString(), UUID.randomUUID().toString()))
                .isInstanceOf(AccessDeniedException.class);

        verify(broadcastRepository, never()).save(any());
    }

    @Test
    void stopBroadcast_OwnerCanStop() {
        broadcast.setStatus(BroadcastStatus.LIVE);
        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(broadcast));
        when(broadcastRepository.save(any(LiveBroadcast.class))).thenAnswer(inv -> inv.getArgument(0));

        broadcastService.stopBroadcast(broadcastId.toString(), userId.toString());

        assertThat(broadcast.getStatus()).isEqualTo(BroadcastStatus.ENDED);
        assertThat(broadcast.getEndedAt()).isNotNull();
        verify(broadcastRepository).save(broadcast);
    }

    @Test
    void getBroadcast_Existing_ReturnsBroadcast() {
        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(broadcast));

        LiveBroadcast result = broadcastService.getBroadcast(broadcastId.toString());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(broadcastId);
    }

    @Test
    void getBroadcast_NonExistent_ThrowsException() {
        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> broadcastService.getBroadcast(broadcastId.toString()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getActiveBroadcasts_ReturnsLiveBroadcasts() {
        when(broadcastRepository.findByStatus(BroadcastStatus.LIVE)).thenReturn(List.of(broadcast));

        List<LiveBroadcast> active = broadcastService.getActiveBroadcasts();

        assertThat(active).hasSize(1);
    }

    @Test
    void deleteBroadcast_OwnerCanDelete() {
        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(broadcast));

        broadcastService.deleteBroadcast(broadcastId.toString(), userId.toString());

        verify(broadcastRepository).delete(broadcast);
    }

    @Test
    void deleteBroadcast_NonOwner_ThrowsException() {
        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(broadcast));

        assertThatThrownBy(() -> broadcastService.deleteBroadcast(
                broadcastId.toString(), UUID.randomUUID().toString()))
                .isInstanceOf(AccessDeniedException.class);

        verify(broadcastRepository, never()).delete(any());
    }

    @Test
    void toDto_ConvertsCorrectly() {
        broadcast.setCreatedAt(LocalDateTime.now());
        broadcast.setStartedAt(LocalDateTime.now());
        broadcast.setViewerCount(42);

        LiveBroadcastDto dto = broadcastService.toDto(broadcast);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(broadcastId.toString());
        assertThat(dto.getTitle()).isEqualTo("Test Broadcast");
        assertThat(dto.getStatus()).isEqualTo("IDLE");
        assertThat(dto.getViewerCount()).isEqualTo(42);
        assertThat(dto.getUserId()).isEqualTo(userId.toString());
    }

    @Test
    void toDto_Null_ReturnsNull() {
        LiveBroadcastDto dto = broadcastService.toDto(null);
        assertThat(dto).isNull();
    }

    @Test
    void onStreamStart_UpdatesStatusToLive() {
        when(broadcastRepository.findByRtmpStreamKey(broadcastId.toString()))
                .thenReturn(Optional.of(broadcast));
        when(broadcastRepository.save(any(LiveBroadcast.class))).thenAnswer(inv -> inv.getArgument(0));

        broadcastService.onStreamStart(broadcastId.toString());

        assertThat(broadcast.getStatus()).isEqualTo(BroadcastStatus.LIVE);
        assertThat(broadcast.getStartedAt()).isNotNull();
        verify(broadcastRepository).save(broadcast);
    }

    @Test
    void onStreamStop_UpdatesStatusToEnded() {
        broadcast.setStatus(BroadcastStatus.LIVE);
        when(broadcastRepository.findByRtmpStreamKey(broadcastId.toString()))
                .thenReturn(Optional.of(broadcast));
        when(broadcastRepository.save(any(LiveBroadcast.class))).thenAnswer(inv -> inv.getArgument(0));

        broadcastService.onStreamStop(broadcastId.toString());

        assertThat(broadcast.getStatus()).isEqualTo(BroadcastStatus.ENDED);
        assertThat(broadcast.getEndedAt()).isNotNull();
        verify(broadcastRepository).save(broadcast);
    }
}
