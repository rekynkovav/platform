package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.EventMaterial;
import com.platform.model.dto.EventMaterialDto;
import com.platform.repository.EventMaterialRepository;
import com.platform.util.RoleUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventMaterialService {

    private final EventMaterialRepository eventMaterialRepository;

    public List<EventMaterialDto> getMaterialsBySection(String sectionId) {
        return eventMaterialRepository.findBySectionIdAndIsActiveTrueOrderBySortOrderAscCreatedAtDesc(sectionId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public EventMaterialDto getMaterial(UUID id) {
        EventMaterial material = eventMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + id));
        return toDto(material);
    }

    @Transactional
    public EventMaterialDto createMaterial(EventMaterialDto.CreateRequest request, Jwt jwt) {
        String userId = jwt.getSubject();
        String userName = jwt.getClaimAsString("preferred_username");

        EventMaterial material = EventMaterial.builder()
                .sectionId(request.getSectionId())
                .type(request.getType())
                .title(request.getTitle())
                .content(request.getContent())
                .url(request.getUrl())
                .mediaId(request.getMediaId())
                .createdBy(userId)
                .authorName(userName)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(true)
                .build();

        material = eventMaterialRepository.save(material);
        log.info("Event material created: {} in section {} by {}", material.getId(), request.getSectionId(), userId);
        return toDto(material);
    }

    @Transactional
    public EventMaterialDto updateMaterial(UUID id, EventMaterialDto.UpdateRequest request, Jwt jwt) {
        EventMaterial material = eventMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + id));

        String userRole = RoleUtils.getUserRole(jwt);
        if (!isAdmin(userRole)) {
            throw new AccessDeniedException("Only admins can update materials");
        }

        if (request.getTitle() != null) material.setTitle(request.getTitle());
        if (request.getContent() != null) material.setContent(request.getContent());
        if (request.getUrl() != null) material.setUrl(request.getUrl());
        if (request.getMediaId() != null) material.setMediaId(request.getMediaId());
        if (request.getSortOrder() != null) material.setSortOrder(request.getSortOrder());
        if (request.getIsActive() != null) material.setIsActive(request.getIsActive());

        material = eventMaterialRepository.save(material);
        log.info("Event material updated: {}", id);
        return toDto(material);
    }

    @Transactional
    public void deleteMaterial(UUID id, Jwt jwt) {
        EventMaterial material = eventMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + id));

        String userRole = RoleUtils.getUserRole(jwt);
        if (!isAdmin(userRole)) {
            throw new AccessDeniedException("Only admins can delete materials");
        }

        eventMaterialRepository.delete(material);
        log.info("Event material deleted: {}", id);
    }

    private EventMaterialDto toDto(EventMaterial m) {
        return EventMaterialDto.builder()
                .id(m.getId())
                .sectionId(m.getSectionId())
                .type(m.getType())
                .title(m.getTitle())
                .content(m.getContent())
                .url(m.getUrl())
                .mediaId(m.getMediaId())
                .createdBy(m.getCreatedBy())
                .authorName(m.getAuthorName())
                .sortOrder(m.getSortOrder())
                .isActive(m.getIsActive())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    private boolean isAdmin(String role) {
        return "MODERATOR_ADMIN".equals(role) || "SUPER_ADMIN".equals(role)
                || "VYACHESLAV".equals(role) || "DEVELOPER".equals(role);
    }
}
