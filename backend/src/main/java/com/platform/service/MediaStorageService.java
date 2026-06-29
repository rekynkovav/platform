package com.platform.service;

import com.platform.model.dto.PostDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import io.micrometer.core.annotation.Timed;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaStorageService {

    private final StorageService storageService;

    @Value("${app.file-upload.max-size:104857600}")
    private long maxFileSize;

    @Timed(value = "media.save", description = "Time to save media")
    public PostDto.MediaUploadResponse saveMedia(MultipartFile file, String type) {
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds " + (maxFileSize / 1024 / 1024) + "MB limit");
        }

        try {
            String mediaType = determineMediaType(file, type);

            String filename = UUID.randomUUID().toString() + "_" +
                    System.currentTimeMillis() + "_" +
                    file.getOriginalFilename().replaceAll("\\s+", "_");

            String storagePath = mediaType + "/" + filename;

            // Используем StorageService для загрузки файла
            String uploadedPath = storageService.upload(
                file.getInputStream(),
                storagePath,
                file.getContentType(),
                file.getSize()
            );

            // Генерируем URL для доступа к файлу
            String url = storageService.generatePresignedUrl(storagePath, 3600);

            log.info("Media saved via StorageService: {}", url);

            return PostDto.MediaUploadResponse.builder()
                    .url(url)
                    .type(mediaType)
                    .size(file.getSize())
                    .filename(filename)
                    .build();

        } catch (IOException e) {
            log.error("Failed to save media", e);
            throw new RuntimeException("Failed to save media: " + e.getMessage());
        }
    }

    private String determineMediaType(MultipartFile file, String type) {
        if (type != null && !type.isEmpty()) {
            return type;
        }

        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.startsWith("image/")) return "image";
            if (contentType.startsWith("video/")) return "video";
        }

        return "other";
    }
}