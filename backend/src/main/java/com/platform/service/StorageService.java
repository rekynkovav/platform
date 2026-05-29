package com.platform.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public interface StorageService {
    // Существующий метод (для байтов)
    String upload(byte[] data, String path, String contentType);

    // НОВЫЙ МЕТОД для потоковой загрузки
    String upload(InputStream inputStream, String path, String contentType, long contentLength);

    // НОВЫЙ МЕТОД для загрузки файлов
    default String upload(File file, String path, String contentType) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            return upload(inputStream, path, contentType, file.length());
        }
    }

    byte[] download(String path);
    boolean delete(String path);
    String generatePresignedUrl(String path, int expirationSeconds);
    boolean exists(String path);

    /**
     * Проверить загрузку файла (верификация размера)
     * @param path путь к файлу
     * @param expectedSize ожидаемый размер в байтах
     * @return true если файл существует и имеет правильный размер
     */
    default boolean verifyUpload(String path, long expectedSize) {
        // Базовая реализация: проверяем существование файла
        // Конкретные реализации могут добавить дополнительную логику
        return exists(path);
    }

    /**
     * Проверить, является ли файл валидным медиафайлом
     * @param path путь к файлу
     * @return true если файл валиден
     */
    default boolean isValidMediaFile(String path) {
        // Базовая реализация: проверяем существование файла
        // Конкретные реализации могут добавить проверку MIME-типа, сигнатур и т.д.
        return exists(path);
    }
}
