package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.EventMaterial;
import com.platform.model.dto.EventMaterialDto;
import com.platform.repository.EventMaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventMaterialServiceTest {

    @Mock private EventMaterialRepository eventMaterialRepository;

    @InjectMocks
    private EventMaterialService eventMaterialService;

    private UUID materialId;
    private EventMaterial material;
    private Jwt adminJwt;
    private Jwt userJwt;

    @BeforeEach
    void setUp() {
        materialId = UUID.randomUUID();
        material = EventMaterial.builder()
                .id(materialId)
                .sectionId("section-1")
                .type("video")
                .title("Introduction")
                .content("Video content")
                .url("https://example.com/video.mp4")
                .createdBy(UUID.randomUUID().toString())
                .authorName("admin")
                .sortOrder(1)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        adminJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", UUID.randomUUID().toString())
                .claim("preferred_username", "admin")
                .claim("realm_access.roles", "SUPER_ADMIN")
                .build();

        userJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", UUID.randomUUID().toString())
                .claim("preferred_username", "user")
                .claim("realm_access.roles", "USER_0")
                .build();
    }

    // ==================== getMaterialsBySection ====================

    @Test
    void getMaterialsBySection_ReturnsList() {
        when(eventMaterialRepository.findBySectionIdAndIsActiveTrueOrderBySortOrderAscCreatedAtDesc("section-1"))
                .thenReturn(List.of(material));

        List<EventMaterialDto> result = eventMaterialService.getMaterialsBySection("section-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Introduction");
        assertThat(result.get(0).getType()).isEqualTo("video");
    }

    @Test
    void getMaterialsBySection_Empty_ReturnsEmptyList() {
        when(eventMaterialRepository.findBySectionIdAndIsActiveTrueOrderBySortOrderAscCreatedAtDesc("empty-section"))
                .thenReturn(List.of());

        List<EventMaterialDto> result = eventMaterialService.getMaterialsBySection("empty-section");

        assertThat(result).isEmpty();
    }

    // ==================== getMaterial ====================

    @Test
    void getMaterial_Exists_ReturnsDto() {
        when(eventMaterialRepository.findById(materialId)).thenReturn(Optional.of(material));

        EventMaterialDto result = eventMaterialService.getMaterial(materialId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(materialId);
        assertThat(result.getSectionId()).isEqualTo("section-1");
    }

    @Test
    void getMaterial_NotFound_ThrowsException() {
        when(eventMaterialRepository.findById(materialId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventMaterialService.getMaterial(materialId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== createMaterial ====================

    @Test
    void createMaterial_SavesAndReturns() {
        EventMaterialDto.CreateRequest request = new EventMaterialDto.CreateRequest();
        request.setSectionId("section-1");
        request.setType("document");
        request.setTitle("Guide");
        request.setContent("PDF guide");
        request.setSortOrder(2);

        when(eventMaterialRepository.save(any(EventMaterial.class))).thenAnswer(inv -> {
            EventMaterial m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        EventMaterialDto result = eventMaterialService.createMaterial(request, adminJwt);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Guide");
        assertThat(result.getType()).isEqualTo("document");
        assertThat(result.getIsActive()).isTrue();
        verify(eventMaterialRepository).save(any(EventMaterial.class));
    }

    @Test
    void createMaterial_NullSortOrder_DefaultsToZero() {
        EventMaterialDto.CreateRequest request = new EventMaterialDto.CreateRequest();
        request.setSectionId("section-1");
        request.setType("text");
        request.setTitle("Note");

        when(eventMaterialRepository.save(any(EventMaterial.class))).thenAnswer(inv -> {
            EventMaterial m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        EventMaterialDto result = eventMaterialService.createMaterial(request, adminJwt);

        assertThat(result.getSortOrder()).isEqualTo(0);
    }

    // ==================== updateMaterial ====================

    @Test
    void updateMaterial_Admin_UpdatesFields() {
        EventMaterialDto.UpdateRequest request = new EventMaterialDto.UpdateRequest();
        request.setTitle("Updated Title");
        request.setSortOrder(5);

        when(eventMaterialRepository.findById(materialId)).thenReturn(Optional.of(material));
        when(eventMaterialRepository.save(any(EventMaterial.class))).thenAnswer(inv -> inv.getArgument(0));

        EventMaterialDto result = eventMaterialService.updateMaterial(materialId, request, adminJwt);

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getSortOrder()).isEqualTo(5);
        verify(eventMaterialRepository).save(material);
    }

    @Test
    void updateMaterial_NonAdmin_ThrowsAccessDenied() {
        EventMaterialDto.UpdateRequest request = new EventMaterialDto.UpdateRequest();
        request.setTitle("Hack");

        // Mock findById so service reaches the role check instead of throwing ResourceNotFoundException
        when(eventMaterialRepository.findById(materialId)).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> eventMaterialService.updateMaterial(materialId, request, userJwt))
                .isInstanceOf(AccessDeniedException.class);

        verify(eventMaterialRepository, never()).save(any());
    }

    @Test
    void updateMaterial_NotFound_ThrowsException() {
        EventMaterialDto.UpdateRequest request = new EventMaterialDto.UpdateRequest();
        when(eventMaterialRepository.findById(materialId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventMaterialService.updateMaterial(materialId, request, adminJwt))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateMaterial_PartialUpdate_OnlySetsNonNull() {
        EventMaterialDto.UpdateRequest request = new EventMaterialDto.UpdateRequest();
        request.setTitle("New Title Only");

        when(eventMaterialRepository.findById(materialId)).thenReturn(Optional.of(material));
        when(eventMaterialRepository.save(any(EventMaterial.class))).thenAnswer(inv -> inv.getArgument(0));

        EventMaterialDto result = eventMaterialService.updateMaterial(materialId, request, adminJwt);

        assertThat(result.getTitle()).isEqualTo("New Title Only");
        assertThat(result.getContent()).isEqualTo("Video content"); // unchanged
    }

    // ==================== deleteMaterial ====================

    @Test
    void deleteMaterial_Admin_Deletes() {
        when(eventMaterialRepository.findById(materialId)).thenReturn(Optional.of(material));

        eventMaterialService.deleteMaterial(materialId, adminJwt);

        verify(eventMaterialRepository).delete(material);
    }

    @Test
    void deleteMaterial_NonAdmin_ThrowsAccessDenied() {
        // Mock findById so service reaches the role check instead of throwing ResourceNotFoundException
        when(eventMaterialRepository.findById(materialId)).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> eventMaterialService.deleteMaterial(materialId, userJwt))
                .isInstanceOf(AccessDeniedException.class);

        verify(eventMaterialRepository, never()).delete(any());
    }

    @Test
    void deleteMaterial_NotFound_ThrowsException() {
        when(eventMaterialRepository.findById(materialId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventMaterialService.deleteMaterial(materialId, adminJwt))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
