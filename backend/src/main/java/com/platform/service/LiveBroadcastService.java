package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.LiveBroadcast;
import com.platform.repository.LiveBroadcastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveBroadcastService {

    private final LiveBroadcastRepository broadcastRepository;

    @Transactional
    public LiveBroadcast createBroadcast(String title, String description, String streamType, String userId) {
        String broadcastId = UUID.randomUUID().toString();
        
        LiveBroadcast broadcast = LiveBroadcast.builder()
            .id(broadcastId)
            .title(title)
            .description(description)
            .streamType(streamType)
            .ownerId(userId)
            .status("idle")
            .rtmpStreamKey(broadcastId)
            .build();
        
        return broadcastRepository.save(broadcast);
    }

    @Transactional
    public String startBroadcast(String broadcastId, String userId) {
        LiveBroadcast broadcast = getBroadcast(broadcastId);
        
        if (!broadcast.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to start this broadcast");
        }
        
        broadcast.setStatus("live");
        broadcast.setStartedAt(LocalDateTime.now());
        broadcastRepository.save(broadcast);
        
        String rtmpUrl = String.format("rtmp://localhost:1935/live/%s", broadcast.getRtmpStreamKey());
        
        log.info("Broadcast started: {} by {}", broadcastId, userId);
        return rtmpUrl;
    }

    @Transactional
    public void stopBroadcast(String broadcastId, String userId) {
        LiveBroadcast broadcast = getBroadcast(broadcastId);
        
        if (!broadcast.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to stop this broadcast");
        }
        
        broadcast.setStatus("ended");
        broadcast.setEndedAt(LocalDateTime.now());
        broadcastRepository.save(broadcast);
        
        log.info("Broadcast stopped: {} by {}", broadcastId, userId);
    }

    public LiveBroadcast getBroadcast(String broadcastId) {
        return broadcastRepository.findById(broadcastId)
            .orElseThrow(() -> new ResourceNotFoundException("Broadcast not found: " + broadcastId));
    }

    public List<LiveBroadcast> getActiveBroadcasts() {
        return broadcastRepository.findByStatus("live");
    }

    @Transactional
    public void deleteBroadcast(String broadcastId, String userId) {
        LiveBroadcast broadcast = getBroadcast(broadcastId);
        
        if (!broadcast.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to delete this broadcast");
        }
        
        broadcastRepository.delete(broadcast);
        log.info("Broadcast deleted: {} by {}", broadcastId, userId);
    }

    public String getHlsPlaylist(String broadcastId) {
        LiveBroadcast broadcast = getBroadcast(broadcastId);
        
        if (!"live".equals(broadcast.getStatus())) {
            throw new IllegalStateException("Broadcast is not live");
        }
        
        // HLS плейлист от Nginx-RTMP
        return String.format("http://localhost:8000/hls/%s/index.m3u8", broadcast.getRtmpStreamKey());
    }

    @Transactional
    public void onStreamStart(String streamKey) {
        broadcastRepository.findByRtmpStreamKey(streamKey).ifPresent(broadcast -> {
            broadcast.setStatus("live");
            broadcast.setStartedAt(LocalDateTime.now());
            broadcastRepository.save(broadcast);
            log.info("RTMP stream started for broadcast: {}", broadcast.getId());
        });
    }

    @Transactional
    public void onStreamStop(String streamKey) {
        broadcastRepository.findByRtmpStreamKey(streamKey).ifPresent(broadcast -> {
            broadcast.setStatus("ended");
            broadcast.setEndedAt(LocalDateTime.now());
            broadcastRepository.save(broadcast);
            log.info("RTMP stream stopped for broadcast: {}", broadcast.getId());
        });
    }

    @Transactional
    public void addViewer(String broadcastId) {
        broadcastRepository.incrementViewerCount(broadcastId);
        
        LiveBroadcast broadcast = getBroadcast(broadcastId);
        int currentViewers = broadcast.getViewerCount() + 1;
        if (currentViewers > broadcast.getMaxViewers()) {
            broadcast.setMaxViewers(currentViewers);
            broadcastRepository.save(broadcast);
        }
    }

    @Transactional
    public void removeViewer(String broadcastId) {
        broadcastRepository.decrementViewerCount(broadcastId);
    }
}
