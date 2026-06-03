package com.platform.controller;

import com.platform.model.dto.EventMaterialDto;
import com.platform.service.EventMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

/**
 * Контроллер материалов разделов EventsHub.
 * Frontend: eventsApi.getMaterialsBySection(), createMaterial(), updateMaterial(), deleteMaterial()
 */
@RestController
@RequestMapping("/api/events/materials")
@RequiredArgsConstructor
public class EventMaterialController {

    private final EventMaterialService eventMaterialService;

    /**
     * Получить материалы раздела
     * Frontend: eventsApi.getMaterialsBySection(sectionId)
     */
    @GetMapping("/{sectionId}")
    public ResponseEntity<List<EventMaterialDto>> getMaterialsBySection(@PathVariable String sectionId) {
        return ResponseEntity.ok(eventMaterialService.getMaterialsBySection(sectionId));
    }

    /**
     * Получить материал по ID
     */
    @GetMapping("/item/{id}")
    public ResponseEntity<EventMaterialDto> getMaterial(@PathVariable UUID id) {
        return ResponseEntity.ok(eventMaterialService.getMaterial(id));
    }

    /**
     * Создать материал
     * Frontend: eventsApi.createMaterial(data)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV', 'DEVELOPER')")
    public ResponseEntity<EventMaterialDto> createMaterial(
            @Valid @RequestBody EventMaterialDto.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(eventMaterialService.createMaterial(request, jwt));
    }

    /**
     * Обновить материал
     * Frontend: eventsApi.updateMaterial(id, data)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV', 'DEVELOPER')")
    public ResponseEntity<EventMaterialDto> updateMaterial(
            @PathVariable UUID id,
            @Valid @RequestBody EventMaterialDto.UpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(eventMaterialService.updateMaterial(id, request, jwt));
    }

    /**
     * Удалить материал
     * Frontend: eventsApi.deleteMaterial(id)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV', 'DEVELOPER')")
    public ResponseEntity<Void> deleteMaterial(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        eventMaterialService.deleteMaterial(id, jwt);
        return ResponseEntity.noContent().build();
    }
}
