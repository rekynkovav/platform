package com.platform.service;

import com.platform.model.dto.PostDto;
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

@Slf4j
@Service
public class MediaStorageService {

    @Value("${app.media.upload-path:/tmp/platform-media}")
    private String uploadPath;

    @Value("${app.media.base-url:https://api.platform.com/media}")
    private String baseUrl;

    @Timed(value = "media.save", description = "Time to save media")
    public PostDto.MediaUploadResponse saveMedia(MultipartFile file, String type) {
        try {
            String mediaType = determineMediaType(file, type);

            Path uploadDir = Paths.get(uploadPath, mediaType);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String filename = UUID.randomUUID().toString() + "_" +
                    System.currentTimeMillis() + "_" +
                    file.getOriginalFilename().replaceAll("\\s+", "_");

            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String url = baseUrl + "/" + mediaType + "/" + filename;

            log.info("Media saved: {}", url);

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