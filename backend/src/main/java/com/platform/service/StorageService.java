package com.platform.service;

public interface StorageService {
    String upload(byte[] data, String path, String contentType);
    byte[] download(String path);
    boolean delete(String path);
    String generatePresignedUrl(String path, int expirationSeconds);
    boolean exists(String path);
}
