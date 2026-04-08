package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.AccessLog;
import com.platform.model.MediaFile;
import com.platform.repository.AccessLogRepository;
import com.platform.repository.MediaFileRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SecureMediaService {

    private final MediaFileRepository mediaFileRepository;
    private final AccessLogRepository accessLogRepository;
    private final S3StorageService s3StorageService;
    private final EncryptionService encryptionService;
    private final WatermarkService watermarkService;
    private final HLSStreamingService hlsStreamingService;

    /**
     * Загрузка и шифрование медиа файла с водяными знаками и HLS
     */
    @Transactional
    public MediaFile uploadSecureMedia(MultipartFile file, String fileType, Jwt jwt, HttpServletRequest request) {
        try {
            String userId = jwt.getSubject();
            String fileId = UUID.randomUUID().toString();

            // Сохраняем временный файл
            java.io.File tempFile = saveTempFile(file);

            // Добавляем водяной знак если это видео или изображение
            java.io.File watermarkedFile = tempFile;
            if ("video".equals(fileType)) {
                watermarkedFile = watermarkService.addVideoWatermark(tempFile, userId, fileId);
            } else if ("image".equals(fileType)) {
                watermarkedFile = watermarkService.addImageWatermark(tempFile, userId, fileId);
            }

            // Генерируем ключ шифрования и IV
            byte[] encryptionKey = encryptionService.generateFileKey(userId, fileId);
            String iv = encryptionService.generateIV();

            // Читаем и шифруем файл
            byte[] originalData = java.nio.file.Files.readAllBytes(watermarkedFile.toPath());
            byte[] encryptedData = encryptionService.encrypt(originalData, encryptionKey, iv);

            // Загружаем зашифрованный файл в S3
            String s3Key = s3StorageService.uploadEncryptedFile(encryptedData, fileType, file.getOriginalFilename());

            // Для видео создаем HLS версию
            String hlsPlaylistUrl = null;
            Integer duration = null;
            if ("video".equals(fileType) && hlsStreamingService != null) {
                try {
                    HLSStreamingService.HLSResult hlsResult = hlsStreamingService.convertToHLS(watermarkedFile, userId, fileId);
                    if (hlsResult != null) {
                        hlsPlaylistUrl = hlsResult.getPlaylistUrl();
                        duration = hlsResult.getDuration();
                    }
                } catch (Exception e) {
                    log.warn("HLS conversion failed, continuing without HLS: {}", e.getMessage());
                }
            }

            // Создаем запись в БД
            MediaFile mediaFile = MediaFile.builder()
                .id(fileId)
                .ownerId(userId)
                .s3Key(s3Key)
                .encryptionIv(iv)
                .fileType(fileType)
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .duration(duration)
                .watermarkData(watermarkService.generateWatermarkData(userId, fileId, fileType))
                .hlsPlaylistUrl(hlsPlaylistUrl)
                .accessLevel("private")
                .viewsCount(0)
                .downloadCount(0)
                .build();

            mediaFile = mediaFileRepository.save(mediaFile);

            // Логируем загрузку
            logAccess(fileId, userId, "UPLOAD", request, true, null);

            // Удаляем временные файлы
            tempFile.delete();
            if (!watermarkedFile.equals(tempFile)) {
                watermarkedFile.delete();
            }

            log.info("Secure media uploaded: {} by {}", fileId, userId);
            return mediaFile;

        } catch (Exception e) {
            log.error("Failed to upload secure media", e);
            throw new RuntimeException("Failed to upload media: " + e.getMessage());
        }
    }

    /**
     * Сохранение временного файла
     */
    private java.io.File saveTempFile(MultipartFile file) throws Exception {
        java.nio.file.Path tempPath = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"),
            "upload_" + UUID.randomUUID() + "_" + file.getOriginalFilename());
        java.nio.file.Files.write(tempPath, file.getBytes());
        return tempPath.toFile();
    }

    /**
     * Генерация временного URL для стриминга
     */
    public String generateStreamUrl(String fileId, Jwt jwt, HttpServletRequest request) {
        String userId = jwt.getSubject();

        MediaFile file = mediaFileRepository.findById(fileId)
            .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

        // Проверяем права доступа
        if (!hasAccess(userId, file, jwt)) {
            logAccess(fileId, userId, "STREAM_REQUEST", request, false, "Access denied");
            throw new AccessDeniedException("No access to this file");
        }

        // Логируем запрос
        logAccess(fileId, userId, "STREAM_REQUEST", request, true, null);

        // Генерируем presigned URL на 5 минут
        String presignedUrl = s3StorageService.generatePresignedUrl(file.getS3Key(), 300);

        // Увеличиваем счетчик просмотров
        mediaFileRepository.incrementViews(fileId);

        return presignedUrl;
    }

    /**
     * Генерация ключа шифрования для HLS сегментов
     */
    public byte[] generateEncryptionKey(String fileId, Jwt jwt, HttpServletRequest request) {
        String userId = jwt.getSubject();

        MediaFile file = mediaFileRepository.findById(fileId)
            .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

        // Проверяем права доступа
        if (!hasAccess(userId, file, jwt)) {
            logAccess(fileId, userId, "KEY_REQUEST", request, false, "Access denied");
            throw new AccessDeniedException("No access to this file");
        }

        // Логируем запрос ключа (важно для отслеживания утечек)
        logAccess(fileId, userId, "KEY_REQUEST", request, true, null);

        // Генерируем ключ для HLS (AES-128)
        return encryptionService.generateHLSKey(userId, fileId);
    }

    /**
     * Скачивание и расшифровка файла (только для SUPER_ADMIN)
     */
    public byte[] downloadAndDecrypt(String fileId, Jwt jwt, HttpServletRequest request) {
        String userId = jwt.getSubject();
        String userRole = getUserRole(jwt);

        MediaFile file = mediaFileRepository.findById(fileId)
            .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

        // КРИТИЧНО: Только SUPER_ADMIN может скачивать расшифрованные оригиналы
        if (!canDownloadOriginal(userRole)) {
            logAccess(fileId, userId, "DOWNLOAD_ATTEMPT", request, false, "Access denied - requires SUPER_ADMIN role");
            throw new AccessDeniedException("Only SUPER_ADMIN can download original files");
        }

        // Проверяем базовые права доступа
        if (!hasAccess(userId, file, jwt)) {
            logAccess(fileId, userId, "DOWNLOAD_ATTEMPT", request, false, "Access denied");
            throw new AccessDeniedException("No access to this file");
        }

        try {
            // Скачиваем зашифрованный файл из S3
            byte[] encryptedData = s3StorageService.downloadFile(file.getS3Key());

            // Генерируем ключ и расшифровываем
            byte[] key = encryptionService.generateFileKey(file.getOwnerId(), fileId);
            byte[] decryptedData = encryptionService.decrypt(encryptedData, key, file.getEncryptionIv());

            // Логируем скачивание с указанием роли
            logAccess(fileId, userId, "DOWNLOAD_ORIGINAL", request, true, "Role: " + userRole);
            mediaFileRepository.incrementDownloads(fileId);

            log.warn("SUPER_ADMIN download: user={}, role={}, file={}", userId, userRole, fileId);

            return decryptedData;

        } catch (Exception e) {
            log.error("Failed to download and decrypt file: {}", fileId, e);
            logAccess(fileId, userId, "DOWNLOAD_ATTEMPT", request, false, e.getMessage());
            throw new RuntimeException("Failed to download file: " + e.getMessage());
        }
    }

    /**
     * Получение информации о файле
     */
    public MediaFile getMediaFile(String fileId, Jwt jwt) {
        String userId = jwt.getSubject();

        MediaFile file = mediaFileRepository.findById(fileId)
            .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

        if (!hasAccess(userId, file, jwt)) {
            throw new AccessDeniedException("No access to this file");
        }

        return file;
    }

    /**
     * Удаление файла
     */
    @Transactional
    public void deleteMediaFile(String fileId, Jwt jwt) {
        String userId = jwt.getSubject();
        String userRole = getUserRole(jwt);

        MediaFile file = mediaFileRepository.findById(fileId)
            .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

        // Только владелец или админ может удалить
        if (!file.getOwnerId().equals(userId) && !isAdmin(userRole)) {
            throw new AccessDeniedException("You don't have permission to delete this file");
        }

        // Удаляем из S3
        s3StorageService.deleteFile(file.getS3Key());

        // Удаляем из БД
        mediaFileRepository.delete(file);

        log.info("Media file deleted: {} by {}", fileId, userId);
    }

    /**
     * Получение файлов пользователя
     */
    public List<MediaFile> getUserFiles(String userId) {
        return mediaFileRepository.findByOwnerId(userId);
    }

    /**
     * Проверка прав доступа к файлу
     */
    private boolean hasAccess(String userId, MediaFile file, Jwt jwt) {
        // Владелец всегда имеет доступ
        if (file.getOwnerId().equals(userId)) {
            return true;
        }

        String userRole = getUserRole(jwt);

        // Админ имеет доступ ко всему
        if (isAdmin(userRole)) {
            return true;
        }

        // Проверяем уровень доступа
        switch (file.getAccessLevel()) {
            case "public":
                return true;
            case "premium":
                return hasPremiumAccess(jwt);
            case "private":
            default:
                return false;
        }
    }

    /**
     * Логирование доступа для аудита
     */
    private void logAccess(String fileId, String userId, String action,
                          HttpServletRequest request, boolean success, String errorMessage) {
        try {
            AccessLog log = AccessLog.builder()
                .fileId(fileId)
                .userId(userId)
                .action(action)
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .success(success)
                .errorMessage(errorMessage)
                .build();

            accessLogRepository.save(log);
        } catch (Exception e) {
            log.error("Failed to log access", e);
        }
    }

    /**
     * Получение IP адреса клиента
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * Извлечение роли пользователя из JWT
     */
    private String getUserRole(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("realm_access.roles");
        if (roles == null) return "user";

        // Приоритет ролей (от высшего к низшему)
        if (roles.contains("super-admin")) return "super-admin";
        if (roles.contains("vyacheslav")) return "vyacheslav";
        if (roles.contains("admin")) return "admin";
        if (roles.contains("moderator")) return "moderator";
        if (roles.contains("sport-admin")) return "sport-admin";
        if (roles.contains("nutrition-admin")) return "nutrition-admin";
        if (roles.contains("consultant-admin")) return "consultant-admin";
        if (roles.contains("creator")) return "creator";
        if (roles.contains("premium")) return "premium";
        if (roles.contains("advanced")) return "advanced";

        return "user";
    }

    /**
     * Проверка админских прав
     */
    private boolean isAdmin(String role) {
        return "admin".equals(role) || "moderator".equals(role) ||
               "super-admin".equals(role) || "vyacheslav".equals(role) ||
               "sport-admin".equals(role) || "nutrition-admin".equals(role) ||
               "consultant-admin".equals(role);
    }

    /**
     * Проверка прав на скачивание оригинала (только SUPER_ADMIN)
     */
    private boolean canDownloadOriginal(String role) {
        return "super-admin".equals(role);
    }

    /**
     * Проверка премиум доступа
     */
    private boolean hasPremiumAccess(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("realm_access.roles");
        return roles != null && (roles.contains("premium") || roles.contains("admin"));
    }
}
