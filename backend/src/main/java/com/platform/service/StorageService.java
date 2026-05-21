package com.platform.service;

import java.io.InputStream;

public interface StorageService {
    // Существующий метод (для байтов)
    String upload(byte[] data, String path, String contentType);

    // НОВЫЙ МЕТОД для потоковой загрузки
    String upload(InputStream inputStream, String path, String contentType, long contentLength);

    byte[] download(String path);
    boolean delete(String path);
    String generatePresignedUrl(String path, int expirationSeconds);
    boolean exists(String path);
}
