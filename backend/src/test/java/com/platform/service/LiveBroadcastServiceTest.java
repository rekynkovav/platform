package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.LiveBroadcast;
import com.platform.repository.LiveBroadcastRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveBroadcastServiceTest {

    @Mock
    private LiveBroadcastRepository broadcastRepository;

    @InjectMocks
    private LiveBroadcastService broadcastService;

    private LiveBroadcast testBroadcast;
    private String userId;

    @BeforeEach
    void setUp() {
        userId = "user123";
        testBroadcast = LiveBroadcast.builder()
            .id("broadcast123")
            .title("Test Broadcast")
            .description("Test Description")
            .streamType("video")
            .ownerId(userId)
            .status("idle")
            .rtmpStreamKey("streamKey123")
            .build();
    }

    @Test
    void testCreateBroadcast() {
        when(broadcastRepository.save(any(LiveBroadcast.class))).thenReturn(testBroadcast);

        LiveBroadcast result = broadcastService.createBroadcast("Test Broadcast", "Test Description", "video", userId);

        assertNotNull(result);
        assertEquals("Test Broadcast", result.getTitle());
        verify(broadcastRepository, times(1)).save(any(LiveBroadcast.class));
    }

    @Test
    void testStartBroadcast() {
        when(broadcastRepository.findById("broadcast123")).thenReturn(Optional.of(testBroadcast));
        when(broadcastRepository.save(any(LiveBroadcast.class))).thenReturn(testBroadcast);

        String rtmpUrl = broadcastService.startBroadcast("broadcast123", userId);

        assertNotNull(rtmpUrl);
        assertTrue(rtmpUrl.contains("rtmp://"));
        assertEquals("live", testBroadcast.getStatus());
        verify(broadcastRepository, times(1)).save(testBroadcast);
    }

    @Test
    void testStartBroadcastAccessDenied() {
        when(broadcastRepository.findById("broadcast123")).thenReturn(Optional.of(testBroadcast));

        assertThrows(AccessDeniedException.class, () -> {
            broadcastService.startBroadcast("broadcast123", "wrongUser");
        });
    }

    @Test
    void testStopBroadcast() {
        when(broadcastRepository.findById("broadcast123")).thenReturn(Optional.of(testBroadcast));
        when(broadcastRepository.save(any(LiveBroadcast.class))).thenReturn(testBroadcast);

        broadcastService.stopBroadcast("broadcast123", userId);

        assertEquals("ended", testBroadcast.getStatus());
        verify(broadcastRepository, times(1)).save(testBroadcast);
    }

    @Test
    void testGetBroadcastNotFound() {
        when(broadcastRepository.findById("invalid")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            broadcastService.getBroadcast("invalid");
        });
    }

    @Test
    void testGetActiveBroadcasts() {
        when(broadcastRepository.findByStatus("live")).thenReturn(List.of(testBroadcast));

        List<LiveBroadcast> active = broadcastService.getActiveBroadcasts();

        assertFalse(active.isEmpty());
        assertEquals(1, active.size());
        verify(broadcastRepository, times(1)).findByStatus("live");
    }

    @Test
    void testDeleteBroadcast() {
        when(broadcastRepository.findById("broadcast123")).thenReturn(Optional.of(testBroadcast));
        doNothing().when(broadcastRepository).delete(testBroadcast);

        broadcastService.deleteBroadcast("broadcast123", userId);

        verify(broadcastRepository, times(1)).delete(testBroadcast);
    }

    @Test
    void testGetHlsPlaylist() {
        testBroadcast.setStatus("live");
        when(broadcastRepository.findById("broadcast123")).thenReturn(Optional.of(testBroadcast));

        String playlist = broadcastService.getHlsPlaylist("broadcast123");

        assertNotNull(playlist);
        assertTrue(playlist.contains(".m3u8"));
    }

    @Test
    void testGetHlsPlaylistNotLive() {
        testBroadcast.setStatus("ended");
        when(broadcastRepository.findById("broadcast123")).thenReturn(Optional.of(testBroadcast));

        assertThrows(IllegalStateException.class, () -> {
            broadcastService.getHlsPlaylist("broadcast123");
        });
    }

    @Test
    void testOnStreamStart() {
        when(broadcastRepository.findByRtmpStreamKey("streamKey123")).thenReturn(Optional.of(testBroadcast));
        when(broadcastRepository.save(any(LiveBroadcast.class))).thenReturn(testBroadcast);

        broadcastService.onStreamStart("streamKey123");

        assertEquals("live", testBroadcast.getStatus());
        verify(broadcastRepository, times(1)).save(testBroadcast);
    }

    @Test
    void testAddViewer() {
        testBroadcast.setViewerCount(0);
        when(broadcastRepository.findById("broadcast123")).thenReturn(Optional.of(testBroadcast));
        doNothing().when(broadcastRepository).incrementViewerCount("broadcast123");
        when(broadcastRepository.save(any(LiveBroadcast.class))).thenReturn(testBroadcast);

        broadcastService.addViewer("broadcast123");

        verify(broadcastRepository, times(1)).incrementViewerCount("broadcast123");
    }
}