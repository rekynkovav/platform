package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.BroadcastStatus;
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

        log.info("Creating broadcast for user {}: title={}", userId, title);
        log.info("Создание трансляции для пользователя {}: название={}", userId, title);

        LiveBroadcast broadcast = LiveBroadcast.builder()
            .id(broadcastId)
            .title(title)
            .description(description)
            .streamType(streamType)
            .ownerId(userId)
            .status(BroadcastStatus.IDLE)
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

        broadcast.setStatus(BroadcastStatus.LIVE);
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

        broadcast.setStatus(BroadcastStatus.ENDED);
        broadcast.setEndedAt(LocalDateTime.now());
        broadcastRepository.save(broadcast);
        
        log.info("Broadcast stopped: {} by {}", broadcastId, userId);
    }

    public LiveBroadcast getBroadcast(String broadcastId) {
        return broadcastRepository.findById(broadcastId)
            .orElseThrow(() -> new ResourceNotFoundException("Broadcast not found: " + broadcastId));
    }

    public List<LiveBroadcast> getActiveBroadcasts() {
        return broadcastRepository.findByStatus(BroadcastStatus.LIVE);
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
        
        if (!BroadcastStatus.LIVE.equals(broadcast.getStatus())) {
            throw new IllegalStateException("Broadcast is not live");
        }
        
        // HLS плейлист от Nginx-RTMP
        return String.format("http://localhost:8000/hls/%s/index.m3u8", broadcast.getRtmpStreamKey());
    }

    @Transactional
    public void onStreamStart(String streamKey) {
        broadcastRepository.findByRtmpStreamKey(streamKey).ifPresent(broadcast -> {
            broadcast.setStatus(BroadcastStatus.LIVE);
            broadcast.setStartedAt(LocalDateTime.now());
            broadcastRepository.save(broadcast);

            log.info("RTMP stream started for broadcast: {}", broadcast.getId());
            log.info("RTMP поток запущен для трансляции: {}", broadcast.getId());
        });
    }

    @Transactional
    public void onStreamStop(String streamKey) {
        broadcastRepository.findByRtmpStreamKey(streamKey).ifPresent(broadcast -> {
            broadcast.setStatus(BroadcastStatus.ENDED);
            broadcast.setEndedAt(LocalDateTime.now());
            broadcastRepository.save(broadcast);

            log.info("RTMP stream stopped for broadcast: {}", broadcast.getId());
            log.info("RTMP поток остановлен для трансляции: {}", broadcast.getId());
        });
    }

    @Transactional
    public void addViewer(String broadcastId) {
        broadcastRepository.incrementViewerCount(broadcastId);
        
        LiveBroadcast broadcast = getBroadcast(broadcastId);
        int currentViewers = broadcast.getViewerCount() != null ? broadcast.getViewerCount() + 1 : 1;
        Integer maxViewers = broadcast.getMaxViewers();
        
        if (maxViewers == null || currentViewers > maxViewers) {
            broadcast.setMaxViewers(currentViewers);
            broadcastRepository.save(broadcast);
        }
    }

    @Transactional
    public void removeViewer(String broadcastId) {
        broadcastRepository.decrementViewerCount(broadcastId);
    }
}
