package com.platform.service;

import com.platform.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaCleanupService {

    private final MediaFileRepository mediaFileRepository;
    private final StorageService storageService;

    @Scheduled(cron = "0 0 4 * * *") // Каждую ночь в 4:00
    @Transactional
    public void cleanupOldHlsSegments() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        var oldSegments = mediaFileRepository.findOldHlsSegments(cutoff);

        for (var segment : oldSegments) {
            try {
                storageService.delete(segment.getStorageKey());
                mediaFileRepository.delete(segment);
                log.info("Deleted old HLS segment: {}", segment.getId());
            } catch (Exception e) {
                log.error("Failed to delete HLS segment: {}", segment.getId(), e);
            }
        }

        log.info("Cleaned up {} old HLS segments", oldSegments.size());
    }
}