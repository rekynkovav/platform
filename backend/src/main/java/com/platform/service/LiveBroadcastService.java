package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.BroadcastStatus;
import com.platform.model.LiveBroadcast;
import com.platform.model.dto.LiveBroadcastDto;
import com.platform.repository.LiveBroadcastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import io.micrometer.core.annotation.Timed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveBroadcastService {

    private final LiveBroadcastRepository broadcastRepository;
    private final BroadcastArchiveService archiveService;

    @Timed(value = "broadcast.create", description = "Time to create broadcast")
    @Transactional(rollbackFor = Exception.class, timeout = 30)
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

    @Timed(value = "broadcast.start", description = "Time to start broadcast")
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    @CacheEvict(value = "broadcasts", allEntries = true)
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

    @Timed(value = "broadcast.stop", description = "Time to stop broadcast")
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    @CacheEvict(value = "broadcasts", allEntries = true)
    public void stopBroadcast(String broadcastId, String userId) {
        LiveBroadcast broadcast = getBroadcast(broadcastId);

        if (!broadcast.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to stop this broadcast");
        }

        broadcast.setStatus(BroadcastStatus.ENDED);
        broadcast.setEndedAt(LocalDateTime.now());
        broadcastRepository.save(broadcast);

        // Асинхронное архивирование
        CompletableFuture.runAsync(() -> archiveService.archiveBroadcast(broadcastId));

        log.info("Broadcast stopped: {} by {}", broadcastId, userId);
    }

    @Timed(value = "broadcast.get", description = "Time to get broadcast")
    @Cacheable(value = "broadcasts", key = "#broadcastId")
    public LiveBroadcast getBroadcast(String broadcastId) {
        return broadcastRepository.findById(broadcastId)
            .orElseThrow(() -> new ResourceNotFoundException("Broadcast not found: " + broadcastId));
    }

    @Timed(value = "broadcast.get_active", description = "Time to get active broadcasts")
    @Transactional(readOnly = true, timeout = 15)
    @Cacheable(value = "broadcasts", key = "'active'", unless = "#result == null || #result.isEmpty()")
    public List<LiveBroadcast> getActiveBroadcasts() {
        return broadcastRepository.findByStatus(BroadcastStatus.LIVE);
    }

    @Timed(value = "broadcast.delete", description = "Time to delete broadcast")
    @Transactional
    @CacheEvict(value = "broadcasts", allEntries = true)
    public void deleteBroadcast(String broadcastId, String userId) {
        LiveBroadcast broadcast = getBroadcast(broadcastId);
        
        if (!broadcast.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to delete this broadcast");
        }
        
        broadcastRepository.delete(broadcast);
        log.info("Broadcast deleted: {} by {}", broadcastId, userId);
    }

    @Timed(value = "broadcast.get_hls", description = "Time to get HLS playlist")
    public String getHlsPlaylist(String broadcastId) {
        LiveBroadcast broadcast = getBroadcast(broadcastId);
        
        if (!BroadcastStatus.LIVE.equals(broadcast.getStatus())) {
            throw new IllegalStateException("Broadcast is not live");
        }
        
        // HLS плейлист от Nginx-RTMP
        return String.format("http://localhost:8000/hls/%s/index.m3u8", broadcast.getRtmpStreamKey());
    }

    @Timed(value = "broadcast.stream_start", description = "Time to handle stream start")
    @Transactional
    @CacheEvict(value = "broadcasts", allEntries = true)
    public void onStreamStart(String streamKey) {
        broadcastRepository.findByRtmpStreamKey(streamKey).ifPresent(broadcast -> {
            broadcast.setStatus(BroadcastStatus.LIVE);
            broadcast.setStartedAt(LocalDateTime.now());
            broadcastRepository.save(broadcast);

            log.info("RTMP stream started for broadcast: {}", broadcast.getId());
            log.info("RTMP поток запущен для трансляции: {}", broadcast.getId());
        });
    }

    @Timed(value = "broadcast.stream_stop", description = "Time to handle stream stop")
    @Transactional
    @CacheEvict(value = "broadcasts", allEntries = true)
    public void onStreamStop(String streamKey) {
        broadcastRepository.findByRtmpStreamKey(streamKey).ifPresent(broadcast -> {
            broadcast.setStatus(BroadcastStatus.ENDED);
            broadcast.setEndedAt(LocalDateTime.now());
            broadcastRepository.save(broadcast);

            log.info("RTMP stream stopped for broadcast: {}", broadcast.getId());
            log.info("RTMP поток остановлен для трансляции: {}", broadcast.getId());
        });
    }

    @Timed(value = "broadcast.add_viewer", description = "Time to add viewer")
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

    /**
     * Преобразовать LiveBroadcast в DTO (безопасный для клиента)
     */
    public LiveBroadcastDto toDto(LiveBroadcast broadcast) {
        if (broadcast == null) {
            return null;
        }

        LiveBroadcastDto dto = new LiveBroadcastDto();
        dto.setId(broadcast.getId());
        dto.setTitle(broadcast.getTitle());
        dto.setDescription(broadcast.getDescription());
        dto.setStreamType(broadcast.getStreamType());
        dto.setUserId(broadcast.getOwnerId());
        dto.setStatus(broadcast.getStatus() != null ? broadcast.getStatus().name() : null);
        dto.setCreatedAt(broadcast.getCreatedAt());
        dto.setStartedAt(broadcast.getStartedAt());
        dto.setEndedAt(broadcast.getEndedAt());
        dto.setViewerCount(broadcast.getViewerCount());
        dto.setHlsPlaylistUrl(broadcast.getHlsPlaylistUrl());
        dto.setThumbnailUrl(broadcast.getThumbnailUrl());

        return dto;
    }

    @Timed(value = "broadcast.remove_viewer", description = "Time to remove viewer")
    @Transactional
    public void removeViewer(String broadcastId) {
        broadcastRepository.decrementViewerCount(broadcastId);
    }
}
