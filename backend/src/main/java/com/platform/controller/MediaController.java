package com.platform.controller;

import com.platform.model.MediaFile;
import com.platform.service.SecureMediaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final SecureMediaService secureMediaService;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ROLE_CREATOR', 'ROLE_ADMIN', 'ROLE_MODERATOR', 'ROLE_SPORT_ADMIN', 'ROLE_NUTRITION_ADMIN', 'ROLE_CONSULTANT_ADMIN', 'ROLE_OWNER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<MediaFile> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        MediaFile mediaFile = secureMediaService.uploadSecureMedia(file, type, jwt, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaFile);
    }

    @GetMapping("/stream/{fileId}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_PREMIUM', 'ROLE_ADVANCED', 'ROLE_CREATOR', 'ROLE_ADMIN', 'ROLE_MODERATOR', 'ROLE_SPORT_ADMIN', 'ROLE_NUTRITION_ADMIN', 'ROLE_CONSULTANT_ADMIN', 'ROLE_OWNER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getStreamUrl(
            @PathVariable String fileId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String url = secureMediaService.generateStreamUrl(fileId, jwt, request);

        return ResponseEntity.ok(Map.of(
            "url", url,
            "expiresIn", 300,
            "type", "presigned"
        ));
    }

    @GetMapping("/key/{fileId}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_PREMIUM', 'ROLE_ADVANCED', 'ROLE_CREATOR', 'ROLE_ADMIN', 'ROLE_MODERATOR', 'ROLE_SPORT_ADMIN', 'ROLE_NUTRITION_ADMIN', 'ROLE_CONSULTANT_ADMIN', 'ROLE_OWNER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<byte[]> getEncryptionKey(
            @PathVariable String fileId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        byte[] key = secureMediaService.generateEncryptionKey(fileId, jwt, request);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
            .header(HttpHeaders.PRAGMA, "no-cache")
            .body(key);
    }

    @GetMapping("/download/{fileId}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_PREMIUM', 'ROLE_ADVANCED', 'ROLE_CREATOR', 'ROLE_ADMIN', 'ROLE_MODERATOR', 'ROLE_SPORT_ADMIN', 'ROLE_NUTRITION_ADMIN', 'ROLE_CONSULTANT_ADMIN', 'ROLE_OWNER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable String fileId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        MediaFile mediaFile = secureMediaService.getMediaFile(fileId, jwt);
        byte[] data = secureMediaService.downloadAndDecrypt(fileId, jwt, request);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(mediaFile.getContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + mediaFile.getOriginalFilename() + "\"")
            .body(data);
    }

    @GetMapping("/{fileId}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_PREMIUM', 'ROLE_ADVANCED', 'ROLE_CREATOR', 'ROLE_ADMIN', 'ROLE_MODERATOR', 'ROLE_SPORT_ADMIN', 'ROLE_NUTRITION_ADMIN', 'ROLE_CONSULTANT_ADMIN', 'ROLE_OWNER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<MediaFile> getMediaInfo(
            @PathVariable String fileId,
            @AuthenticationPrincipal Jwt jwt) {

        MediaFile mediaFile = secureMediaService.getMediaFile(fileId, jwt);
        return ResponseEntity.ok(mediaFile);
    }

    @GetMapping("/my-files")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_PREMIUM', 'ROLE_ADVANCED', 'ROLE_CREATOR', 'ROLE_ADMIN', 'ROLE_MODERATOR', 'ROLE_SPORT_ADMIN', 'ROLE_NUTRITION_ADMIN', 'ROLE_CONSULTANT_ADMIN', 'ROLE_OWNER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<MediaFile>> getMyFiles(
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        List<MediaFile> files = secureMediaService.getUserFiles(userId);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/hls/{fileId}/playlist.m3u8")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_PREMIUM', 'ROLE_ADVANCED', 'ROLE_CREATOR', 'ROLE_ADMIN', 'ROLE_MODERATOR', 'ROLE_SPORT_ADMIN', 'ROLE_NUTRITION_ADMIN', 'ROLE_CONSULTANT_ADMIN', 'ROLE_OWNER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<String> getHLSPlaylist(
            @PathVariable String fileId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        MediaFile mediaFile = secureMediaService.getMediaFile(fileId, jwt);

        if (mediaFile.getHlsPlaylistUrl() == null) {
            return ResponseEntity.notFound().build();
        }

        String playlistUrl = secureMediaService.generateStreamUrl(fileId, jwt, request);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
            .header(HttpHeaders.CACHE_CONTROL, "no-cache")
            .body(playlistUrl);
    }

    @DeleteMapping("/{fileId}")
    @PreAuthorize("hasAnyRole('ROLE_CREATOR', 'ROLE_ADMIN', 'ROLE_MODERATOR', 'ROLE_SPORT_ADMIN', 'ROLE_NUTRITION_ADMIN', 'ROLE_CONSULTANT_ADMIN', 'ROLE_OWNER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String fileId,
            @AuthenticationPrincipal Jwt jwt) {

        secureMediaService.deleteMediaFile(fileId, jwt);
        return ResponseEntity.noContent().build();
    }
}
