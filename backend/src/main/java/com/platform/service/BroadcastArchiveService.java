package com.platform.service;

import com.platform.exception.ResourceNotFoundException;
import com.platform.model.LiveBroadcast;
import com.platform.model.BroadcastStatus;
import com.platform.repository.LiveBroadcastRepository;
import com.platform.util.FFmpegExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastArchiveService {

    private final LiveBroadcastRepository broadcastRepository;
    private final StorageService storageService;
    private final FFmpegExecutor ffmpegExecutor;

    private static final String ARCHIVE_PREFIX = "archive/";
    private static final String HLS_TEMP_PREFIX = "/tmp/hls/";

    /**
     * Архивирование стрима после окончания
     */
    @Transactional
    public void archiveBroadcast(String broadcastId) {
        if (broadcastId == null || broadcastId.isBlank()) {
            throw new IllegalArgumentException("Broadcast ID is required");
        }

        LiveBroadcast broadcast = broadcastRepository.findById(UUID.fromString(broadcastId))
                .orElseThrow(() -> new ResourceNotFoundException("Broadcast not found: " + broadcastId));

        if (broadcast.getRecordedUrl() != null && !broadcast.getRecordedUrl().isBlank()) {
            log.info("Broadcast already archived: {}", broadcastId);
            cleanupHlsIfArchived(broadcast);
            return;
        }

        if (broadcast.getStatus() != BroadcastStatus.ENDED) {
            throw new IllegalStateException("Cannot archive live broadcast: " + broadcastId);
        }

        String contentType = detectContentType(broadcast);
        String hlsPath = HLS_TEMP_PREFIX + broadcast.getRtmpStreamKey();
        File playlistFile = new File(hlsPath + "/playlist.m3u8");

        if (!playlistFile.exists()) {
            log.warn("No HLS files found for broadcast: {}", broadcastId);
            return;
        }

        File outputFile = null;
        String s3Key = null;

        try {
            if ("video".equals(contentType)) {
                outputFile = convertHlsToMp4(hlsPath, broadcastId);
                s3Key = ARCHIVE_PREFIX + broadcastId + "/recording.mp4";
            } else {
                outputFile = convertHlsToM4a(hlsPath, broadcastId);
                s3Key = ARCHIVE_PREFIX + broadcastId + "/recording.m4a";
            }

            if (outputFile == null || !outputFile.exists() || outputFile.length() == 0) {
                throw new RuntimeException("Conversion produced empty or invalid file");
            }

            String mimeType = "video".equals(contentType) ? "video/mp4" : "audio/mp4";
            storageService.upload(outputFile, s3Key, mimeType);

            if (!storageService.verifyUpload(s3Key, outputFile.length())) {
                throw new RuntimeException("S3 upload verification failed");
            }

            if (!storageService.isValidMediaFile(s3Key)) {
                throw new RuntimeException("Uploaded file is not accessible or invalid");
            }

            broadcast.setRecordedUrl(s3Key);
            broadcast.setRecordedAt(LocalDateTime.now());
            broadcast.setRecordedSize(outputFile.length());
            broadcast.setRecordedDuration(ffmpegExecutor.probeDuration(outputFile));
            broadcastRepository.save(broadcast);

            log.info("Broadcast archived successfully: {} -> {}, size={} bytes",
                broadcastId, s3Key, outputFile.length());

            cleanupHlsSegments(hlsPath);

        } catch (Exception e) {
            log.error("Archive failed for broadcast: {}", broadcastId, e);
            throw new RuntimeException("Archive failed: " + e.getMessage(), e);

        } finally {
            if (outputFile != null && outputFile.exists()) {
                outputFile.delete();
            }
        }
    }

    /**
     * Получение URL для просмотра архива
     */
    public String getArchiveStreamUrl(String broadcastId, String userId) {
        LiveBroadcast broadcast = broadcastRepository.findById(UUID.fromString(broadcastId))
                .orElseThrow(() -> new ResourceNotFoundException("Broadcast not found: " + broadcastId));

        if (broadcast.getRecordedUrl() == null) {
            throw new ResourceNotFoundException("Archive not available for broadcast: " + broadcastId);
        }

        return storageService.generatePresignedUrl(broadcast.getRecordedUrl(), 3600);
    }

    /**
     * Проверка, можно ли удалить HLS после успешного архивирования
     */
    private void cleanupHlsIfArchived(LiveBroadcast broadcast) {
        String s3Key = broadcast.getRecordedUrl();
        String hlsPath = HLS_TEMP_PREFIX + broadcast.getRtmpStreamKey();

        if (storageService.isValidMediaFile(s3Key)) {
            cleanupHlsSegments(hlsPath);
            log.info("Cleaned up HLS segments for already archived broadcast: {}", broadcast.getId());
        } else {
            log.warn("Archived broadcast {} has invalid S3 file, keeping HLS segments", broadcast.getId());
        }
    }

    /**
     * Конвертация HLS сегментов в MP4
     */
    private File convertHlsToMp4(String hlsPath, String broadcastId) throws Exception {
        String playlistPath = hlsPath + "/playlist.m3u8";
        String outputPath = "/tmp/archive_" + broadcastId + ".mp4";

        List<String> command = List.of(
            "ffmpeg", "-i", playlistPath,
            "-c", "copy",
            "-bsf:a", "aac_adtstoasc",
            outputPath
        );

        ffmpegExecutor.execute(command);
        return new File(outputPath);
    }

    /**
     * Конвертация аудио-стрима в M4A
     */
    private File convertHlsToM4a(String hlsPath, String broadcastId) throws Exception {
        String playlistPath = hlsPath + "/playlist.m3u8";
        String outputPath = "/tmp/archive_audio_" + broadcastId + ".m4a";

        List<String> command = List.of(
            "ffmpeg", "-i", playlistPath,
            "-c:a", "aac",
            "-b:a", "128k",
            "-vn",
            outputPath
        );

        ffmpegExecutor.execute(command);

        File outputFile = new File(outputPath);
        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new RuntimeException("Conversion produced empty file");
        }

        return outputFile;
    }

    /**
     * Определение типа контента по HLS плейлисту
     */
    private String detectContentType(LiveBroadcast broadcast) {
        return "video";
    }

    /**
     * Безопасное удаление HLS сегментов
     */
    private void cleanupHlsSegments(String hlsPath) {
        try {
            Path path = Paths.get(hlsPath);
            if (Files.exists(path)) {
                Files.walk(path)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            log.warn("Failed to delete: {}", p);
                        }
                    });
                log.info("Cleaned up HLS files: {}", hlsPath);
            }
        } catch (IOException e) {
            log.error("Failed to cleanup HLS files: {}", hlsPath, e);
        }
    }

    /**
     * Периодическая очистка старых HLS сегментов
     */
    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void cleanupStaleHlsSegments() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        List<LiveBroadcast> endedBroadcasts = broadcastRepository.findByStatus(BroadcastStatus.ENDED);

        List<LiveBroadcast> staleBroadcasts = endedBroadcasts.stream()
            .filter(broadcast -> broadcast.getEndedAt() != null && broadcast.getEndedAt().isBefore(cutoff))
            .collect(Collectors.toList());

        for (LiveBroadcast broadcast : staleBroadcasts) {
            if (broadcast.getRecordedUrl() == null) {
                archiveBroadcast(broadcast.getId().toString());
            } else {
                String hlsPath = HLS_TEMP_PREFIX + broadcast.getRtmpStreamKey();
                cleanupHlsSegments(hlsPath);
            }
        }
    }
}
