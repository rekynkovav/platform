package com.platform.controller;

import com.platform.model.dto.ArchiveDto;
import com.platform.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/archives")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    @GetMapping
    public ResponseEntity<List<ArchiveDto>> getAllArchives(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String before) {

        return ResponseEntity.ok(archiveService.getAllArchives(limit, before));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArchiveDto> getArchive(@PathVariable String id) {
        ArchiveDto archive = archiveService.getArchive(id);
        archiveService.incrementViews(id);
        return ResponseEntity.ok(archive);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<ArchiveDto>> getArchivesByEvent(@PathVariable String eventId) {
        return ResponseEntity.ok(archiveService.getArchivesByEvent(eventId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    public ResponseEntity<ArchiveDto> createArchive(
            @Valid @RequestBody ArchiveDto.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(archiveService.createArchive(request, jwt));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    public ResponseEntity<ArchiveDto> updateArchive(
            @PathVariable String id,
            @Valid @RequestBody ArchiveDto.UpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(archiveService.updateArchive(id, request, jwt));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    public ResponseEntity<Void> deleteArchive(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        archiveService.deleteArchive(id, jwt);
        return ResponseEntity.noContent().build();
    }
}
