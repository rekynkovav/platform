package com.platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class WatermarkService {

    @Value("${media.watermark.enabled:true}")
    private boolean watermarkEnabled;

    @Value("${media.watermark.font-size:24}")
    private int fontSize;

    @Value("${media.watermark.opacity:0.5}")
    private double opacity;

    @Value("${app.media.upload-path:/tmp/platform-media}")
    private String tempPath;

    /**
     * Добавление водяного знака на видео
     */
    public File addVideoWatermark(File inputVideo, String userId, String fileId) {
        if (!watermarkEnabled) {
            return inputVideo;
        }

        try {
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String watermarkText = String.format("User: %s | %s | ID: %s",
                userId.substring(0, Math.min(8, userId.length())),
                timestamp,
                fileId.substring(0, 8));

            Path outputPath = Paths.get(tempPath, "watermarked_" + UUID.randomUUID() + ".mp4");
            Files.createDirectories(outputPath.getParent());

            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-i");
            command.add(inputVideo.getAbsolutePath());
            command.add("-vf");
            command.add(String.format(
                "drawtext=text='%s':fontsize=%d:fontcolor=white@%.1f:x=(w-text_w)/2:y=h-th-10:box=1:boxcolor=black@0.3:boxborderw=5",
                watermarkText, fontSize, opacity
            ));
            command.add("-codec:a");
            command.add("copy");
            command.add("-y");
            command.add(outputPath.toString());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("FFmpeg failed with exit code: {}", exitCode);
                return inputVideo;
            }

            log.info("Video watermark added: {}", outputPath);
            return outputPath.toFile();

        } catch (Exception e) {
            log.error("Failed to add video watermark", e);
            return inputVideo;
        }
    }

    /**
     * Добавление водяного знака на изображение
     */
    public File addImageWatermark(File inputImage, String userId, String fileId) {
        if (!watermarkEnabled) {
            return inputImage;
        }

        try {
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String watermarkText = String.format("User: %s | %s",
                userId.substring(0, Math.min(8, userId.length())),
                timestamp);

            Path outputPath = Paths.get(tempPath, "watermarked_" + UUID.randomUUID() + ".jpg");
            Files.createDirectories(outputPath.getParent());

            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-i");
            command.add(inputImage.getAbsolutePath());
            command.add("-vf");
            command.add(String.format(
                "drawtext=text='%s':fontsize=%d:fontcolor=white@%.1f:x=10:y=h-th-10:box=1:boxcolor=black@0.3:boxborderw=5",
                watermarkText, fontSize, opacity
            ));
            command.add("-y");
            command.add(outputPath.toString());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("FFmpeg failed with exit code: {}", exitCode);
                return inputImage;
            }

            log.info("Image watermark added: {}", outputPath);
            return outputPath.toFile();

        } catch (Exception e) {
            log.error("Failed to add image watermark", e);
            return inputImage;
        }
    }

    /**
     * Генерация JSON с данными водяного знака для хранения в БД
     */
    public String generateWatermarkData(String userId, String fileId, String fileType) {
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return String.format(
            "{\"userId\":\"%s\",\"fileId\":\"%s\",\"timestamp\":\"%s\",\"type\":\"%s\",\"position\":\"bottom-center\"}",
            userId, fileId, timestamp, fileType
        );
    }

    /**
     * Проверка доступности FFmpeg
     */
    public boolean isFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.warn("FFmpeg not available: {}", e.getMessage());
            return false;
        }
    }
}
