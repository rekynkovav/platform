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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SecureMediaService {

    private final MediaFileRepository mediaFileRepository;
    private final AccessLogRepository accessLogRepository;
    private final StorageService storageService;
    private final EncryptionService encryptionService;
    private final WatermarkService watermarkService;
    private final HLSStreamingService hlsStreamingService;

    @Transactional
    public MediaFile uploadSecureMedia(MultipartFile file, String fileType, Jwt jwt, HttpServletRequest request) {
        try {
            String userId = jwt.getSubject();
            String fileId = UUID.randomUUID().toString();

            File tempFile = saveTempFile(file);
            File watermarkedFile = tempFile;

            if ("video".equals(fileType)) {
                watermarkedFile = watermarkService.addVideoWatermark(tempFile, userId, fileId);
            } else if ("image".equals(fileType)) {
                watermarkedFile = watermarkService.addImageWatermark(tempFile, userId, fileId);
            }

            byte[] encryptionKey = encryptionService.generateFileKey(userId, fileId);
            String iv = encryptionService.generateIV();

            byte[] originalData = Files.readAllBytes(watermarkedFile.toPath());
            byte[] encryptedData = encryptionService.encrypt(originalData, encryptionKey, iv);

            String storageKey = storageService.upload(encryptedData,
                String.format("%s/%s_%s", fileType, fileId, file.getOriginalFilename()),
                file.getContentType());

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

            MediaFile mediaFile = MediaFile.builder()
                .id(fileId)
                .ownerId(userId)
                .s3Key(storageKey)
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

            logAccess(fileId, userId, "UPLOAD", request, true, null);

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

    private File saveTempFile(MultipartFile file) throws Exception {
        Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"),
            "upload_" + UUID.randomUUID() + "_" + file.getOriginalFilename());
        Files.write(tempPath, file.getBytes());
        return tempPath.toFile();
    }

    public String generateStreamUrl(String fileId, Jwt jwt, HttpServletRequest request) {
        String userId = jwt.getSubject();

        MediaFile file = mediaFileRepository.findById(fileId)
            .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

        if (!hasAccess(userId, file, jwt)) {
            logAccess(fileId, userId, "STREAM_REQUEST", request, false, "Access denied");
            throw new AccessDeniedException("No access to this file");
        }

        logAccess(fileId, userId, "STREAM_REQUEST", request, true, null);
        String presignedUrl = storageService.generatePresignedUrl(file.getS3Key(), 300);
        mediaFileRepository.incrementViews(fileId);

        return presignedUrl;
    }

    public byte[] generateEncryptionKey(String fileId, Jwt jwt, HttpServletRequest request) {
        String userId = jwt.getSubject();

        MediaFile file = mediaFileRepository.findById(fileId)
            .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

        if (!hasAccess(userId, file, jwt)) {
            logAccess(fileId, userId, "KEY_REQUEST", request, false, "Access denied");
            throw new AccessDeniedException("No access to this file");
        }

        logAccess(fileId, userId, "KEY_REQUEST", request, true, null);
        return encryptionService.generateHLSKey(userId, fileId);
    }

    public byte[] downloadAndDecrypt(String fileId, Jwt jwt, HttpServletRequest request) {
        String userId = jwt.getSubject();
        String userRole = getUserRole(jwt);

        MediaFile file = mediaFileRepository.findById(fileId)
            .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

        if (!canDownloadOriginal(userRole)) {
            logAccess(fileId, userId, "DOWNLOAD_ATTEMPT", request, false, "Access denied - requires SUPER_ADMIN role");
            throw new AccessDeniedException("Only SUPER_ADMIN can download original files");
        }

        if (!hasAccess(userId, file, jwt)) {
            logAccess(fileId, userId, "DOWNLOAD_ATTEMPT", request, false, "Access denied");
            throw new AccessDeniedException("No access to this file");
        }

        try {
            byte[] encryptedData = storageService.download(file.getS3Key());
            byte[] key = encryptionService.generateFileKey(file.getOwnerId(), fileId);
            byte[] decryptedData = encryptionService.decrypt(encryptedData, key, file.getEncryptionIv());

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

    public MediaFile getMediaFile(String fileId, Jwt jwt) {
        String userId = jwt.getSubject();

        MediaFile file = mediaFileRepository.findById(fileId)
            .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

        if (!hasAccess(userId, file, jwt)) {
            throw new AccessDeniedException("No access to this file");
        }

        return file;
    }

    @Transactional
    public void deleteMediaFile(String fileId, Jwt jwt) {
        String userId = jwt.getSubject();
        String userRole = getUserRole(jwt);

        MediaFile file = mediaFileRepository.findById(fileId)
            .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

        if (!file.getOwnerId().equals(userId) && !isAdmin(userRole)) {
            throw new AccessDeniedException("You don't have permission to delete this file");
        }

        storageService.delete(file.getS3Key());
        mediaFileRepository.delete(file);

        log.info("Media file deleted: {} by {}", fileId, userId);
    }

    public List<MediaFile> getUserFiles(String userId) {
        return mediaFileRepository.findByOwnerId(userId);
    }

    private boolean hasAccess(String userId, MediaFile file, Jwt jwt) {
        if (file.getOwnerId().equals(userId)) {
            return true;
        }

        String userRole = getUserRole(jwt);

        if (isAdmin(userRole)) {
            return true;
        }

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

    private void logAccess(String fileId, String userId, String action,
                          HttpServletRequest request, boolean success, String errorMessage) {
        try {
            AccessLog accessLog = AccessLog.builder()
                .fileId(fileId)
                .userId(userId)
                .action(action)
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .success(success)
                .errorMessage(errorMessage)
                .build();

            accessLogRepository.save(accessLog);
        } catch (Exception e) {
            log.error("Failed to log access", e);
        }
    }

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

    private String getUserRole(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("realm_access.roles");
        if (roles == null) return "USER_0";

        if (roles.contains("SUPER_ADMIN")) return "SUPER_ADMIN";
        if (roles.contains("VYACHESLAV")) return "VYACHESLAV";
        if (roles.contains("DEVELOPER")) return "DEVELOPER";
        if (roles.contains("MODERATOR_ADMIN")) return "MODERATOR_ADMIN";
        if (roles.contains("CONSULTANT_SPORT")) return "CONSULTANT_SPORT";
        if (roles.contains("CONSULTANT_PITANIE")) return "CONSULTANT_PITANIE";
        if (roles.contains("USER_2")) return "USER_2";
        if (roles.contains("USER_1")) return "USER_1";
        return "USER_0";
    }

    private boolean isAdmin(String role) {
        return "SUPER_ADMIN".equals(role) || 
               "VYACHESLAV".equals(role) || 
               "MODERATOR_ADMIN".equals(role) ||
               "DEVELOPER".equals(role);
    }

    private boolean canDownloadOriginal(String role) {
        return "SUPER_ADMIN".equals(role);
    }

    private boolean hasPremiumAccess(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("realm_access.roles");
        return roles != null && (roles.contains("USER_2") || roles.contains("PREMIUM"));
    }
}
