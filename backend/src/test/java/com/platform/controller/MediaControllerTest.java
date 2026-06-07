package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.exception.GlobalExceptionHandler;
import com.platform.model.MediaFile;
import com.platform.service.SecureMediaService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static com.platform.controller.TestSecurityHelper.securityContextFilter;
import static com.platform.controller.TestSecurityHelper.withAuth;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MediaControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private SecureMediaService secureMediaService;
    @InjectMocks private MediaController mediaController;

    private String userId;
    private String fileId;
    private Jwt userJwt;
    private Jwt superAdminJwt;
    private JwtAuthenticationToken userAuth;
    private JwtAuthenticationToken superAdminAuth;
    private MediaFile mediaFile;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(mediaController)
                .addFilters(securityContextFilter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        userId = UUID.randomUUID().toString();
        fileId = UUID.randomUUID().toString();

        userJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("preferred_username", "user")
                .claim("realm_access.roles", "USER_0")
                .build();

        superAdminJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("preferred_username", "superadmin")
                .claim("realm_access.roles", "SUPER_ADMIN")
                .build();

        userAuth = new JwtAuthenticationToken(userJwt, List.of(new SimpleGrantedAuthority("ROLE_USER_0")));
        superAdminAuth = new JwtAuthenticationToken(superAdminJwt, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));

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
                .hlsPlaylistUrl("https://cdn.example.com/hls/playlist.m3u8")
                .build();
    }

    // ==================== POST /api/media/upload ====================

    @Test
    void uploadMedia_Authenticated_ReturnsCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", "fake-video-content".getBytes());

        when(secureMediaService.uploadSecureMedia(any(), eq("video"), any(Jwt.class), any(HttpServletRequest.class)))
                .thenReturn(mediaFile);

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("type", "video")
                        .with(withAuth(userAuth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(fileId))
                .andExpect(jsonPath("$.fileType").value("video"));
    }

    // ==================== GET /api/media/stream/{fileId} ====================

    @Test
    void getStreamUrl_ReturnsPresignedUrl() throws Exception {
        when(secureMediaService.generateStreamUrl(eq(fileId), any(Jwt.class), any(HttpServletRequest.class)))
                .thenReturn("https://s3.example.com/presigned");

        mockMvc.perform(get("/api/media/stream/{fileId}", fileId)
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://s3.example.com/presigned"))
                .andExpect(jsonPath("$.expiresIn").value(300))
                .andExpect(jsonPath("$.type").value("presigned"));
    }

    // ==================== GET /api/media/key/{fileId} ====================

    @Test
    void getEncryptionKey_ReturnsBinaryWithNoCacheHeaders() throws Exception {
        byte[] key = new byte[]{1, 2, 3, 4, 5};
        when(secureMediaService.generateEncryptionKey(eq(fileId), any(Jwt.class), any(HttpServletRequest.class)))
                .thenReturn(key);

        mockMvc.perform(get("/api/media/key/{fileId}", fileId)
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(content().bytes(key));
    }

    // ==================== GET /api/media/download/{fileId} ====================

    @Test
    void downloadFile_SuperAdmin_ReturnsDecryptedContent() throws Exception {
        byte[] decrypted = new byte[]{10, 20, 30};
        when(secureMediaService.getMediaFile(eq(fileId), any(Jwt.class))).thenReturn(mediaFile);
        when(secureMediaService.downloadAndDecrypt(eq(fileId), any(Jwt.class), any(HttpServletRequest.class)))
                .thenReturn(decrypted);

        mockMvc.perform(get("/api/media/download/{fileId}", fileId)
                        .with(withAuth(superAdminAuth)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("test.mp4")))
                .andExpect(content().bytes(decrypted));
    }

    // ==================== GET /api/media/{fileId} ====================

    @Test
    void getMediaInfo_ReturnsFileMetadata() throws Exception {
        when(secureMediaService.getMediaFile(eq(fileId), any(Jwt.class))).thenReturn(mediaFile);

        mockMvc.perform(get("/api/media/{fileId}", fileId)
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fileId))
                .andExpect(jsonPath("$.originalFilename").value("test.mp4"));
    }

    // ==================== GET /api/media/my-files ====================

    @Test
    void getMyFiles_ReturnsList() throws Exception {
        when(secureMediaService.getUserFiles(userId)).thenReturn(List.of(mediaFile));

        mockMvc.perform(get("/api/media/my-files")
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(fileId));
    }

    @Test
    void getMyFiles_Empty_ReturnsEmptyArray() throws Exception {
        when(secureMediaService.getUserFiles(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/media/my-files")
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ==================== GET /api/media/hls/{fileId}/playlist.m3u8 ====================

    @Test
    void getHLSPlaylist_HasUrl_ReturnsM3u8() throws Exception {
        when(secureMediaService.getMediaFile(eq(fileId), any(Jwt.class))).thenReturn(mediaFile);
        when(secureMediaService.generateStreamUrl(eq(fileId), any(Jwt.class), any(HttpServletRequest.class)))
                .thenReturn("https://cdn.example.com/hls/playlist.m3u8");

        mockMvc.perform(get("/api/media/hls/{fileId}/playlist.m3u8", fileId)
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("mpegurl")))
                .andExpect(content().string("https://cdn.example.com/hls/playlist.m3u8"));
    }

    @Test
    void getHLSPlaylist_NoHlsUrl_Returns404() throws Exception {
        mediaFile.setHlsPlaylistUrl(null);
        when(secureMediaService.getMediaFile(eq(fileId), any(Jwt.class))).thenReturn(mediaFile);

        mockMvc.perform(get("/api/media/hls/{fileId}/playlist.m3u8", fileId)
                        .with(withAuth(userAuth)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE /api/media/{fileId} ====================

    @Test
    void deleteFile_OwnerCanDelete_Returns204() throws Exception {
        doNothing().when(secureMediaService).deleteMediaFile(eq(fileId), any(Jwt.class));

        mockMvc.perform(delete("/api/media/{fileId}", fileId)
                        .with(withAuth(userAuth)))
                .andExpect(status().isNoContent());

        verify(secureMediaService).deleteMediaFile(fileId, userJwt);
    }
}
