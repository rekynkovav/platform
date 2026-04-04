package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Archive;
import com.platform.model.dto.ArchiveDto;
import com.platform.repository.ArchiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
public class ArchiveService {

    private final ArchiveRepository archiveRepository;

    public List<ArchiveDto> getAllArchives(int limit, String before) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Archive> archives;

        if (before != null && !before.isEmpty()) {
            archives = archiveRepository.findByStartTimeBefore(before, pageable);
        } else {
            archives = archiveRepository.findAllByOrderByStartTimeDesc(pageable);
        }

        return archives.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ArchiveDto getArchive(String id) {
        Archive archive = archiveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Archive not found: " + id));
        return toDto(archive);
    }

    public List<ArchiveDto> getArchivesByEvent(String eventId) {
        List<Archive> archives = archiveRepository.findByEventId(eventId);
        return archives.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ArchiveDto createArchive(ArchiveDto.CreateRequest request,
                                    org.springframework.security.oauth2.jwt.Jwt jwt) {
        String userId = jwt.getSubject();

        Archive archive = Archive.builder()
                .id(UUID.randomUUID().toString())
                .title(request.getTitle())
                .description(request.getDescription())
                .videoUrl(request.getVideoUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .duration(request.getDuration())
                .viewsCount(0)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .eventId(request.getEventId())
                .createdAt(LocalDateTime.now())
                .build();

        archive = archiveRepository.save(archive);
        log.info("Archive created: {} by {}", archive.getId(), userId);

        return toDto(archive);
    }

    @Transactional
    public ArchiveDto updateArchive(String id, ArchiveDto.UpdateRequest request,
                                    org.springframework.security.oauth2.jwt.Jwt jwt) {
        String userId = jwt.getSubject();
        String userRole = getUserRole(jwt);

        Archive archive = archiveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Archive not found: " + id));

        if (!isAdmin(userRole)) {
            throw new AccessDeniedException("Only admin can update archives");
        }

        if (request.getTitle() != null) archive.setTitle(request.getTitle());
        if (request.getDescription() != null) archive.setDescription(request.getDescription());
        if (request.getVideoUrl() != null) archive.setVideoUrl(request.getVideoUrl());
        if (request.getThumbnailUrl() != null) archive.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getDuration() != null) archive.setDuration(request.getDuration());
        if (request.getEventId() != null) archive.setEventId(request.getEventId());

        archive.setUpdatedAt(LocalDateTime.now());
        archive = archiveRepository.save(archive);

        log.info("Archive updated: {} by {}", id, userId);
        return toDto(archive);
    }

    @Transactional
    public void deleteArchive(String id, org.springframework.security.oauth2.jwt.Jwt jwt) {
        String userId = jwt.getSubject();
        String userRole = getUserRole(jwt);

        if (!isAdmin(userRole)) {
            throw new AccessDeniedException("Only admin can delete archives");
        }

        archiveRepository.deleteById(id);
        log.info("Archive deleted: {} by {}", id, userId);
    }

    @Transactional
    public void incrementViews(String id) {
        archiveRepository.incrementViews(id);
        log.debug("Archive views incremented: {}", id);
    }

    private ArchiveDto toDto(Archive archive) {
        return ArchiveDto.builder()
                .id(archive.getId())
                .title(archive.getTitle())
                .description(archive.getDescription())
                .videoUrl(archive.getVideoUrl())
                .thumbnailUrl(archive.getThumbnailUrl())
                .duration(archive.getDuration())
                .viewsCount(archive.getViewsCount())
                .startTime(archive.getStartTime())
                .endTime(archive.getEndTime())
                .eventId(archive.getEventId())
                .createdAt(archive.getCreatedAt())
                .build();
    }

    private String getUserRole(org.springframework.security.oauth2.jwt.Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("realm_access.roles");
        if (roles == null) return "user";
        if (roles.contains("admin")) return "admin";
        if (roles.contains("moderator")) return "moderator";
        return "user";
    }

    private boolean isAdmin(String role) {
        return "admin".equals(role) || "moderator".equals(role);
    }
}
