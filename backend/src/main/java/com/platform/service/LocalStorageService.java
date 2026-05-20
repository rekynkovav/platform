package com.platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@Profile("!prod")
public class LocalStorageService implements StorageService {

    @Value("${app.storage.local-path:./uploads}")
    private String storagePath;

    @Override
    public String upload(byte[] data, String path, String contentType) {
        try {
            Path fullPath = Paths.get(storagePath, path);
            Path parent = fullPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.write(fullPath, data);
            log.info("File uploaded locally: {}", fullPath);
            return fullPath.toString();
        } catch (IOException e) {
            log.error("Failed to upload file locally: {}", path, e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Override
    public byte[] download(String path) {
        try {
            Path fullPath = Paths.get(storagePath, path);
            if (!Files.exists(fullPath)) {
                throw new RuntimeException("File not found: " + path);
            }
            return Files.readAllBytes(fullPath);
        } catch (IOException e) {
            log.error("Failed to download file: {}", path, e);
            throw new RuntimeException("Failed to download file", e);
        }
    }

    @Override
    public boolean delete(String path) {
        try {
            Path fullPath = Paths.get(storagePath, path);
            return Files.deleteIfExists(fullPath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", path, e);
            return false;
        }
    }

    @Override
    public String generatePresignedUrl(String path, int expirationSeconds) {
        return "/local-storage/" + path + "?expires=" + (System.currentTimeMillis() + expirationSeconds * 1000L);
    }

    @Override
    public boolean exists(String path) {
        Path fullPath = Paths.get(storagePath, path);
        return Files.exists(fullPath);
    }
}
