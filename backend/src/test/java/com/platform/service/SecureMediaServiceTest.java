package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.MediaFile;
import com.platform.repository.AccessLogRepository;
import com.platform.repository.MediaFileRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecureMediaServiceTest {

    @Mock private MediaFileRepository mediaFileRepository;
    @Mock private AccessLogRepository accessLogRepository;
    @Mock private StorageService storageService;
    @Mock private EncryptionService encryptionService;
    @Mock private WatermarkService watermarkService;
    @Mock private HLSStreamingService hlsStreamingService;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private SecureMediaService secureMediaService;

    private String fileId;
    private String userId;
    private String adminId;
    private MediaFile mediaFile;
    private Jwt userJwt;
    private Jwt adminJwt;
    private Jwt superAdminJwt;
    private Jwt premiumJwt;

    @BeforeEach
    void setUp() {
        fileId = UUID.randomUUID().toString();
        userId = UUID.randomUUID().toString();
        adminId = UUID.randomUUID().toString();

        mediaFile = MediaFile.builder()
                .id(fileId)
                .ownerId(userId)
                .s3Key("video/" + fileId + "/test.mp4")
                .encryptionIv("iv123")
                .fileType("video")
                .originalFilename("test.mp4")
                .contentType("video/mp4")
                .fileSize(1024L)
                .accessLevel("private")
                .viewsCount(0)
                .downloadCount(0)
                .build();

        userJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("realm_access", Map.of("roles", List.of("USER_0")))
                .build();

        adminJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", adminId)
                .claim("realm_access", Map.of("roles", List.of("MODERATOR_ADMIN")))
                .build();

        superAdminJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", adminId)
                .claim("realm_access", Map.of("roles", List.of("SUPER_ADMIN")))
                .build();

        premiumJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("realm_access", Map.of("roles", List.of("USER_2")))
                .build();

        lenient().when(request.getHeader("User-Agent")).thenReturn("TestAgent");
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    // ==================== generateStreamUrl ====================

    @Test
    void generateStreamUrl_OwnerHasAccess_ReturnsPresignedUrl() {
        when(mediaFileRepository.findById(fileId)).thenReturn(Optional.of(mediaFile));
        when(storageService.generatePresignedUrl(mediaFile.getS3Key(), 300))
                .thenReturn("https://s3.example.com/presigned");
        when(accessLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String url = secureMediaService.generateStreamUrl(fileId, userJwt, request);

        assertThat(url).isEqualTo("https://s3.example.com/presigned");
        verify(mediaFileRepository).incrementViews(fileId);
    }

    @Test
    void generateStreamUrl_AdminHasAccess_ReturnsUrl() {
        when(mediaFileRepository.findById(fileId)).thenReturn(Optional.of(mediaFile));
        when(storageService.generatePresignedUrl(mediaFile.getS3Key(), 300))
                .thenReturn("https://s3.example.com/admin-url");
        when(accessLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String url = secureMediaService.generateStreamUrl(fileId, adminJwt, request);

        assertThat(url).isNotNull();
    }

    @Test
    void generateStreamUrl_NonOwnerPrivate_ThrowsAccessDenied() {
        String otherUserId = UUID.randomUUID().toString();
        Jwt otherJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", otherUserId)
                .claim("realm_access", Map.of("roles", List.of("USER_0")))
                .build();

        when(mediaFileRepository.findById(fileId)).thenReturn(Optional.of(mediaFile));
        when(accessLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> secureMediaService.generateStreamUrl(fileId, otherJwt, request))
                .isInstanceOf(AccessDeniedException.class);

        verify(mediaFileRepository, never()).incrementViews(anyString());
    }

    @Test
    void generateStreamUrl_PublicFile_AnyoneCanAccess() {
        mediaFile.setAccessLevel("public");
        String otherUserId = UUID.randomUUID().toString();
        Jwt otherJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", otherUserId)
                .claim("realm_access", Map.of("roles", List.of("USER_0")))
                .build();

        when(mediaFileRepository.findById(fileId)).thenReturn(Optional.of(mediaFile));
        when(storageService.generatePresignedUrl(mediaFile.getS3Key(), 300))
                .thenReturn("https://s3.example.com/public");
        when(accessLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String url = secureMediaService.generateStreamUrl(fileId, otherJwt, request);

        assertThat(url).isNotNull();
    }

    @Test
    void generateStreamUrl_PremiumFile_PremiumUserCanAccess() {
        mediaFile.setAccessLevel("premium");
        when(mediaFileRepository.findById(fileId)).thenReturn(Optional.of(mediaFile));
        when(storageService.generatePresignedUrl(mediaFile.getS3Key(), 300))
                .thenReturn("https://s3.example.com/premium");
        when(accessLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String url = secureMediaService.generateStreamUrl(fileId, premiumJwt, request);

        assertThat(url).isNotNull();
    }

    @Test
    void generateStreamUrl_FileNotFound_ThrowsException() {
        when(mediaFileRepository.findById(fileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> secureMediaService.generateStreamUrl(fileId, userJwt, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== getMediaFile ====================

    @Test
    void getMediaFile_OwnerReturnsFile() {
        when(mediaFileRepository.findById(fileId)).thenReturn(Optional.of(mediaFile));

        MediaFile result = secureMediaService.getMediaFile(fileId, userJwt);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(fileId);
    }

    @Test
    void getMediaFile_NonOwnerPrivate_ThrowsAccessDenied() {
        String otherUserId = UUID.randomUUID().toString();
        Jwt otherJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", otherUserId)
                .claim("realm_access", Map.of("roles", List.of("USER_0")))
                .build();

        when(mediaFileRepository.findById(fileId)).thenReturn(Optional.of(mediaFile));

        assertThatThrownBy(() -> secureMediaService.getMediaFile(fileId, otherJwt))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ==================== deleteMediaFile ====================

    @Test
    void deleteMediaFile_OwnerCanDelete() {
        when(mediaFileRepository.findById(fileId)).thenReturn(Optional.of(mediaFile));

        secureMediaService.deleteMediaFile(fileId, userJwt);

        verify(storageService).delete(mediaFile.getS3Key());
        verify(mediaFileRepository).delete(mediaFile);
    }

    @Test
    void deleteMediaFile_AdminCanDelete() {
        when(mediaFileRepository.findById(fileId)).thenReturn(Optional.of(mediaFile));

        secureMediaService.deleteMediaFile(fileId, adminJwt);

        verify(storageService).delete(mediaFile.getS3Key());
        verify(mediaFileRepository).delete(mediaFile);
    }

    @Test
    void deleteMediaFile_NonOwnerNonAdmin_ThrowsAccessDenied() {
        String otherUserId = UUID.randomUUID().toString();
        Jwt otherJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", otherUserId)
                .claim("realm_access", Map.of("roles", List.of("USER_0")))
                .build();

        when(mediaFileRepository.findById(fileId)).thenReturn(Optional.of(mediaFile));

        assertThatThrownBy(() -> secureMediaService.deleteMediaFile(fileId, otherJwt))
                .isInstanceOf(AccessDeniedException.class);

        verify(storageService, never()).delete(anyString());
        verify(mediaFileRepository, never()).delete(any());
    }

    @Test
    void deleteMediaFile_NotFound_ThrowsException() {
        when(mediaFileRepository.findById(fileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> secureMediaService.deleteMediaFile(fileId, userJwt))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== downloadAndDecrypt ====================

    @Test
    void downloadAndDecrypt_SuperAdmin_CanDownload() throws Exception {
        byte[] encrypted = new byte[]{1, 2, 3};
        byte[] key = new byte[]{4, 5, 6};
        byte[] decrypted = new byte[]{7, 8, 9};

        when(mediaFileRepository.findById(fileId)).thenReturn(Optional.of(mediaFile));
        when(storageService.download(mediaFile.getS3Key())).thenReturn(encrypted);
        when(encryptionService.generateFileKey(userId, fileId)).thenReturn(key);
        when(encryptionService.decrypt(encrypted, key, "iv123")).thenReturn(decrypted);
        when(accessLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] result = secureMediaService.downloadAndDecrypt(fileId, superAdminJwt, request);

        assertThat(result).isEqualTo(decrypted);
        verify(mediaFileRepository).incrementDownloads(fileId);
    }

    @Test
    void downloadAndDecrypt_ModeratorAdmin_ThrowsAccessDenied() {
        when(mediaFileRepository.findById(fileId)).thenReturn(Optional.of(mediaFile));
        when(accessLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> secureMediaService.downloadAndDecrypt(fileId, adminJwt, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SUPER_ADMIN");
    }

    @Test
    void downloadAndDecrypt_RegularUser_ThrowsAccessDenied() {
        when(mediaFileRepository.findById(fileId)).thenReturn(Optional.of(mediaFile));
        when(accessLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> secureMediaService.downloadAndDecrypt(fileId, userJwt, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ==================== getUserFiles ====================

    @Test
    void getUserFiles_ReturnsList() {
        when(mediaFileRepository.findByOwnerId(userId)).thenReturn(List.of(mediaFile));

        List<MediaFile> result = secureMediaService.getUserFiles(userId);

        assertThat(result).hasSize(1);
    }

    @Test
    void getUserFiles_Empty_ReturnsEmptyList() {
        when(mediaFileRepository.findByOwnerId(userId)).thenReturn(List.of());

        List<MediaFile> result = secureMediaService.getUserFiles(userId);

        assertThat(result).isEmpty();
    }
}
