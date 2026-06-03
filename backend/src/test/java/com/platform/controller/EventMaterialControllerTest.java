package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.exception.GlobalExceptionHandler;
import com.platform.model.dto.EventMaterialDto;
import com.platform.service.EventMaterialService;
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
class EventMaterialControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private EventMaterialService eventMaterialService;
    @InjectMocks private EventMaterialController eventMaterialController;

    private String userId;
    private UUID materialId;
    private String sectionId;
    private Jwt adminJwt;
    private Jwt userJwt;
    private JwtAuthenticationToken adminAuth;
    private JwtAuthenticationToken userAuth;
    private EventMaterialDto materialDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventMaterialController)
                .addFilters(securityContextFilter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        userId = UUID.randomUUID().toString();
        materialId = UUID.randomUUID();
        sectionId = UUID.randomUUID().toString();

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

        materialDto = EventMaterialDto.builder()
                .id(materialId)
                .sectionId(sectionId)
                .title("Workout Plan")
                .content("Weekly workout schedule")
                .type("document")
                .url("https://storage.example.com/materials/plan.pdf")
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== GET /api/events/materials/{sectionId} ====================

    @Test
    void getMaterialsBySection_ReturnsList() throws Exception {
        when(eventMaterialService.getMaterialsBySection(sectionId))
                .thenReturn(List.of(materialDto));

        mockMvc.perform(get("/api/events/materials/{sectionId}", sectionId)
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Workout Plan"));
    }

    @Test
    void getMaterialsBySection_Empty_ReturnsEmptyArray() throws Exception {
        when(eventMaterialService.getMaterialsBySection(sectionId))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/events/materials/{sectionId}", sectionId)
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ==================== GET /api/events/materials/item/{id} ====================

    @Test
    void getMaterial_Exists_ReturnsOk() throws Exception {
        when(eventMaterialService.getMaterial(materialId)).thenReturn(materialDto);

        mockMvc.perform(get("/api/events/materials/item/{id}", materialId)
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("document"))
                .andExpect(jsonPath("$.url").exists());
    }

    // ==================== POST /api/events/materials ====================

    @Test
    void createMaterial_AdminCanCreate() throws Exception {
        EventMaterialDto.CreateRequest request = new EventMaterialDto.CreateRequest();
        request.setSectionId(sectionId);
        request.setTitle("New Material");
        request.setContent("Description");
        request.setType("video");
        request.setUrl("https://storage.example.com/video.mp4");

        when(eventMaterialService.createMaterial(any(EventMaterialDto.CreateRequest.class), any(Jwt.class)))
                .thenReturn(materialDto);

        mockMvc.perform(post("/api/events/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Workout Plan"));
    }

    // ==================== PUT /api/events/materials/{id} ====================

    @Test
    void updateMaterial_AdminCanUpdate() throws Exception {
        EventMaterialDto.UpdateRequest request = new EventMaterialDto.UpdateRequest();
        request.setTitle("Updated Title");

        EventMaterialDto updated = EventMaterialDto.builder()
                .id(materialId).sectionId(sectionId).title("Updated Title")
                .content("Desc").type("document")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(eventMaterialService.updateMaterial(eq(materialId), any(EventMaterialDto.UpdateRequest.class), any(Jwt.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/events/materials/{id}", materialId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    // ==================== DELETE /api/events/materials/{id} ====================

    @Test
    void deleteMaterial_AdminCanDelete_Returns204() throws Exception {
        doNothing().when(eventMaterialService).deleteMaterial(eq(materialId), any(Jwt.class));

        mockMvc.perform(delete("/api/events/materials/{id}", materialId)
                        .with(withAuth(adminAuth)))
                .andExpect(status().isNoContent());
    }
}
