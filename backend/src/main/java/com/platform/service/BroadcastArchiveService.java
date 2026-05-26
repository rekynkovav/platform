package com.platform.service;

import com.platform.exception.ResourceNotFoundException;
import com.platform.model.LiveBroadcast;
import com.platform.repository.LiveBroadcastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastArchiveService {

    private final LiveBroadcastRepository broadcastRepository;
    private final S3StorageService s3StorageService;
    private final LocalStorageService localStorageService;

    private static final String ARCHIVE_PREFIX = "archive/";
    private static final String HLS_TEMP_PREFIX = "/hls/";

    /**
     * Архивирование стрима после окончания
     */
    @Transactional
    public void archiveBroadcast(String broadcastId) {
        // 1. Валидация входных данных
        if (broadcastId == null || broadcastId.isBlank()) {
            throw new IllegalArgumentException("Broadcast ID is required");
        }

        // 2. Загрузка стрима
        LiveBroadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new ResourceNotFoundException("Broadcast not found: " + broadcastId));

        // 3. Проверка, не архивирован ли уже
        if (broadcast.getRecordedUrl() != null && !broadcast.getRecordedUrl().isBlank()) {
            log.info("Broadcast already archived: {}", broadcastId);
            // Всё равно проверяем, можно ли удалить HLS
            cleanupHlsIfArchived(broadcast);
            return;
        }

        // 4. Проверка, что стрим закончился
        if (broadcast.getStatus() != LiveBroadcast.BroadcastStatus.ENDED) {
            throw new IllegalStateException("Cannot archive live broadcast: " + broadcastId);
        }

        // 5. Определение типа контента (видео или аудио)
        String contentType = detectContentType(broadcast);
        String hlsPath = HLS_TEMP_PREFIX + broadcast.getRtmpStreamKey();
        File playlistFile = new File(hlsPath + "/playlist.m3u8");

        if (!playlistFile.exists()) {
            log.warn("No HLS files found for broadcast: {}", broadcastId);
            return;
        }

        // 6. Конвертация в MP4 (видео) или M4A (аудио)
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

            // 7. Проверка, что конвертация прошла успешно
            if (outputFile == null || !outputFile.exists() || outputFile.length() == 0) {
                throw new RuntimeException("Conversion produced empty or invalid file");
            }

            // 8. Загрузка в S3
            String mimeType = "video".equals(contentType) ? "video/mp4" : "audio/mp4";
            s3StorageService.upload(outputFile, s3Key, mimeType);

            // 9. ПРОВЕРКА: файл действительно загружен
            if (!s3StorageService.verifyUpload(s3Key, outputFile.length())) {
                throw new RuntimeException("S3 upload verification failed");
            }

            // 10. ПРОВЕРКА: файл доступен и валиден
            if (!s3StorageService.isValidMediaFile(s3Key)) {
                throw new RuntimeException("Uploaded file is not accessible or invalid");
            }

            // 11. ТОЛЬКО ПОСЛЕ ВСЕХ ПРОВЕРОК — обновляем БД
            broadcast.setRecordedUrl(s3Key);
            broadcast.setRecordedAt(LocalDateTime.now());
            broadcast.setRecordedSize(outputFile.length());
            broadcast.setRecordedDuration(getDuration(outputFile));
            broadcastRepository.save(broadcast);

            log.info("Broadcast archived successfully: {} -> {}, size={} bytes",
                broadcastId, s3Key, outputFile.length());

            // 12. ТОЛЬКО ПОСЛЕ УСПЕШНОГО СОХРАНЕНИЯ В БД — удаляем HLS
            cleanupHlsSegments(hlsPath);

        } catch (Exception e) {
            log.error("Archive failed for broadcast: {}", broadcastId, e);
            // НЕ удаляем HLS сегменты при ошибке
            throw new RuntimeException("Archive failed: " + e.getMessage(), e);

        } finally {
            // Очистка временного файла
            if (outputFile != null && outputFile.exists()) {
                outputFile.delete();
            }
        }
    }

    /**
     * Получение URL для просмотра архива
     */
    public String getArchiveStreamUrl(String broadcastId, String userId) {
        LiveBroadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new ResourceNotFoundException("Broadcast not found: " + broadcastId));

        if (broadcast.getRecordedUrl() == null) {
            throw new ResourceNotFoundException("Archive not available for broadcast: " + broadcastId);
        }

        // Прегенерированная ссылка на 1 час
        return s3StorageService.generatePresignedUrl(broadcast.getRecordedUrl(), 3600);
    }

    /**
     * Проверка, можно ли удалить HLS после успешного архивирования
     */
    private void cleanupHlsIfArchived(LiveBroadcast broadcast) {
        String s3Key = broadcast.getRecordedUrl();
        String hlsPath = HLS_TEMP_PREFIX + broadcast.getRtmpStreamKey();

        // Проверяем, что файл в S3 существует и валиден
        if (s3StorageService.isValidMediaFile(s3Key)) {
            cleanupHlsSegments(hlsPath);
            log.info("Cleaned up HLS segments for already archived broadcast: {}", broadcast.getId());
        } else {
            log.warn("Archived broadcast {} has invalid S3 file, keeping HLS segments", broadcast.getId());
        }
    }

    /**
     * Конвертация HLS сегментов в MP4
     */
    private File convertHlsToMp4(String hlsPath, String broadcastId) throws IOException, InterruptedException {
        String playlistPath = hlsPath + "/playlist.m3u8";
        String outputPath = "/tmp/archive_" + broadcastId + ".mp4";

        ProcessBuilder pb = new ProcessBuilder(
            "ffmpeg", "-i", playlistPath,
            "-c", "copy",
            "-bsf:a", "aac_adtstoasc",
            outputPath
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg conversion failed with exit code: " + exitCode);
        }

        return new File(outputPath);
    }

    /**
     * Конвертация аудио-стрима в M4A
     */
    private File convertHlsToM4a(String hlsPath, String broadcastId) throws IOException, InterruptedException {
        String playlistPath = hlsPath + "/playlist.m3u8";
        String outputPath = "/tmp/archive_audio_" + broadcastId + ".m4a";

        ProcessBuilder pb = new ProcessBuilder(
            "ffmpeg", "-i", playlistPath,
            "-c:a", "aac",
            "-b:a", "128k",
            "-vn",  // Без видео
            outputPath
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg conversion failed with exit code: " + exitCode);
        }

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
        // По умолчанию — видео
        // Можно анализировать первый сегмент
        return "video";
    }

    /**
     * Получение длительности из MP4 файла
     */
    private int getDuration(File mp4File) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
            "ffprobe", "-v", "error",
            "-show_entries", "format=duration",
            "-of", "default=noprint_wrappers=1:nokey=1",
            mp4File.getAbsolutePath()
        );
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            if (line != null) {
                return (int) Double.parseDouble(line);
            }
        }
        return 0;
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
        List<LiveBroadcast> staleBroadcasts = broadcastRepository
            .findByStatusAndEndedAtBefore(LiveBroadcast.BroadcastStatus.ENDED, cutoff);

        for (LiveBroadcast broadcast : staleBroadcasts) {
            if (broadcast.getRecordedUrl() == null) {
                // Не архивирован — архивируем сейчас
                archiveBroadcast(broadcast.getId());
            } else {
                // Уже архивирован — просто чистим сегменты
                String hlsPath = HLS_TEMP_PREFIX + broadcast.getRtmpStreamKey();
                cleanupHlsSegments(hlsPath);
            }
        }
    }
}