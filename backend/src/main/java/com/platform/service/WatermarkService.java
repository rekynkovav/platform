package com.platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.Future;

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

    @Value("${app.watermark.font-path:/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf}")
    private String fontPath;

    @Value("${app.watermark.temp-path:/tmp/watermarks}")
    private String tempPath;

    @PostConstruct
    public void checkFFmpeg() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {}
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("FFMPEG NOT AVAILABLE! Install: apt-get install ffmpeg");
            } else {
                log.info("FFmpeg is available");
            }
        } catch (Exception e) {
            log.error("FFMPEG NOT FOUND! Watermark and HLS will not work. Error: {}", e.getMessage());
        }
    }

    /**
     * Добавление невидимого водяного знака на видео
     * Знак содержит UID пользователя + ID файла + timestamp
     * Накладывается с низкой прозрачностью (0.1) и маленьким шрифтом
     */
    @Async("ffmpegExecutor")
    public Future<File> addVideoWatermark(File inputVideo, String userId, String fileId) {
        try {
            // Генерируем уникальный watermark текст
            String watermarkText = generateWatermarkText(userId, fileId);

            // Создаём временный файл для вывода
            Path outputPath = Paths.get(tempPath, "watermarked_" + UUID.randomUUID() + ".mp4");
            Files.createDirectories(outputPath.getParent());
            File outputFile = outputPath.toFile();

            // Команда FFmpeg для наложения текста (почти невидимый)
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-i");
            command.add(inputVideo.getAbsolutePath());
            command.add("-vf");
            command.add(String.format(
                "drawtext=text='%s':fontfile=%s:fontsize=12:fontcolor=white@0.1:x=10:y=10:box=0:shadowx=0:shadowy=0",
                watermarkText, fontPath
            ));
            command.add("-codec:a");
            command.add("copy");
            command.add("-y");
            command.add(outputFile.getAbsolutePath());

            log.info("Adding watermark to video: {}", inputVideo.getName());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("FFmpeg watermark failed: {}", output);
                // Если FFmpeg не доступен, возвращаем исходный файл
                return new AsyncResult<>(inputVideo);
            }

            log.info("Watermark added successfully: {}", outputFile.getAbsolutePath());
            return new AsyncResult<>(outputFile);

        } catch (Exception e) {
            log.error("Failed to add watermark to video", e);
            return new org.springframework.scheduling.annotation.AsyncResult<>(inputVideo);
        }
    }

    /**
     * Добавление невидимого водяного знака на изображение
     */
    @Async("ffmpegExecutor")
    public Future<File> addImageWatermark(File inputImage, String userId, String fileId) {
        try {
            String watermarkText = generateWatermarkText(userId, fileId);

            Path outputPath = Paths.get(tempPath, "watermarked_" + UUID.randomUUID() + ".jpg");
            Files.createDirectories(outputPath.getParent());
            File outputFile = outputPath.toFile();

            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-i");
            command.add(inputImage.getAbsolutePath());
            command.add("-vf");
            command.add(String.format(
                "drawtext=text='%s':fontfile=%s:fontsize=10:fontcolor=white@0.1:x=5:y=5",
                watermarkText, fontPath
            ));
            command.add("-y");
            command.add(outputFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Image watermark failed: {}", output);
                return new AsyncResult<>(inputImage);
            }

            return new AsyncResult<>(outputFile);

        } catch (Exception e) {
            log.error("Failed to add watermark to image", e);
            return new AsyncResult<>(inputImage);
        }
    }

    /**
     * Генерация текста водяного знака
     * Формат: UID:userId|FID:fileId|TS:timestamp
     */
    private String generateWatermarkText(String userId, String fileId) {
        long timestamp = System.currentTimeMillis();
        // Кодируем в base64 чтобы скрыть от глаз
        String raw = String.format("UID:%s|FID:%s|TS:%d", userId, fileId, timestamp);
        return raw;
    }

    /**
     * Генерация метаданных водяного знака для хранения в БД
     */
    public String generateWatermarkData(String userId, String fileId, String fileType) {
        return String.format("{\"userId\":\"%s\",\"fileId\":\"%s\",\"type\":\"%s\",\"timestamp\":%d}",
            userId, fileId, fileType, System.currentTimeMillis());
    }

    /**
     * Извлечение водяного знака из видео (для обнаружения утечки)
     * Используется SUPER_ADMIN для расследования
     */
    public String extractWatermark(File videoFile) {
        try {
            // Используем ffprobe для извлечения текста (сложно)
            // Базовая реализация: возвращаем заглушку
            log.info("Extracting watermark from: {}", videoFile.getName());
            return "watermark_data_placeholder";
        } catch (Exception e) {
            log.error("Failed to extract watermark", e);
            return null;
        }
    }
}
