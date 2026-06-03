package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.exception.GlobalExceptionHandler;
import com.platform.model.dto.ArchiveDto;
import com.platform.service.ArchiveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.platform.controller.TestSecurityHelper.securityContextFilter;
import static com.platform.controller.TestSecurityHelper.withAuth;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArchiveControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private ArchiveService archiveService;
    @InjectMocks private ArchiveController archiveController;

    private String userId;
    private String archiveId;
    private Jwt adminJwt;
    private Jwt userJwt;
    private JwtAuthenticationToken adminAuth;
    private JwtAuthenticationToken userAuth;
    private ArchiveDto archiveDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(archiveController)
                .addFilters(securityContextFilter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        userId = UUID.randomUUID().toString();
        archiveId = UUID.randomUUID().toString();

        adminJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("preferred_username", "admin")
                .claim("realm_access.roles", "SUPER_ADMIN")
                .build();

        userJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("preferred_username", "user")
                .claim("realm_access.roles", "USER_0")
                .build();

        adminAuth = new JwtAuthenticationToken(adminJwt, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        userAuth = new JwtAuthenticationToken(userJwt, List.of(new SimpleGrantedAuthority("ROLE_USER_0")));

        archiveDto = ArchiveDto.builder()
                .id(archiveId)
                .title("Workout Session #1")
                .description("Full body workout")
                .videoUrl("https://storage.example.com/video.mp4")
                .thumbnailUrl("https://storage.example.com/thumb.jpg")
                .duration(3600)
                .viewsCount(150)
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().minusHours(1))
                .eventId(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAll_ReturnsList() throws Exception {
        when(archiveService.getAllArchives(anyInt(), any())).thenReturn(List.of(archiveDto));

        mockMvc.perform(get("/api/archives")
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Workout Session #1"));
    }

    @Test
    void getAll_Empty_ReturnsEmptyArray() throws Exception {
        when(archiveService.getAllArchives(anyInt(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/archives")
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getById_Exists_ReturnsOk() throws Exception {
        when(archiveService.getArchive(archiveId)).thenReturn(archiveDto);

        mockMvc.perform(get("/api/archives/{id}", archiveId)
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(archiveId))
                .andExpect(jsonPath("$.duration").value(3600))
                .andExpect(jsonPath("$.viewsCount").value(150));
    }

    @Test
    void getById_NotFound_Returns404() throws Exception {
        String unknownId = UUID.randomUUID().toString();
        when(archiveService.getArchive(unknownId))
                .thenThrow(new com.platform.exception.ResourceNotFoundException("Archive not found: " + unknownId));

        mockMvc.perform(get("/api/archives/{id}", unknownId)
                        .with(withAuth(userAuth)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_AdminCanCreate() throws Exception {
        ArchiveDto.CreateRequest request = ArchiveDto.CreateRequest.builder()
                .title("New Session")
                .description("Description")
                .videoUrl("https://storage.example.com/new.mp4")
                .duration(1800)
                .build();

        when(archiveService.createArchive(any(ArchiveDto.CreateRequest.class), any(Jwt.class)))
                .thenReturn(archiveDto);

        mockMvc.perform(post("/api/archives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Workout Session #1"));
    }

    @Test
    void update_AdminCanUpdate() throws Exception {
        ArchiveDto.UpdateRequest request = ArchiveDto.UpdateRequest.builder()
                .title("Updated Title")
                .build();

        ArchiveDto updated = ArchiveDto.builder()
                .id(archiveId).title("Updated Title")
                .description("Desc").duration(3600)
                .createdAt(LocalDateTime.now()).build();

        when(archiveService.updateArchive(eq(archiveId), any(ArchiveDto.UpdateRequest.class), any(Jwt.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/archives/{id}", archiveId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void delete_AdminCanDelete_Returns204() throws Exception {
        doNothing().when(archiveService).deleteArchive(eq(archiveId), any(Jwt.class));

        mockMvc.perform(delete("/api/archives/{id}", archiveId)
                        .with(withAuth(adminAuth)))
                .andExpect(status().isNoContent());
    }
}
