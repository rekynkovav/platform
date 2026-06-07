package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Archive;
import com.platform.model.dto.ArchiveDto;
import com.platform.repository.ArchiveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArchiveService Unit Tests")
class ArchiveServiceTest {

    @Mock
    private ArchiveRepository archiveRepository;

    @InjectMocks
    private ArchiveService archiveService;

    private Archive testArchive;
    private Jwt adminJwt;
    private Jwt superAdminJwt;
    private Jwt userJwt;
    private Jwt moderatorJwt;

    @BeforeEach
    void setUp() {
        testArchive = Archive.builder()
                .id("archive-1")
                .title("Test Archive")
                .description("Archive description")
                .videoUrl("https://cdn.example.com/video.mp4")
                .thumbnailUrl("https://cdn.example.com/thumb.jpg")
                .duration(3600)
                .viewsCount(100)
                .startTime(LocalDateTime.now().minusDays(5))
                .endTime(LocalDateTime.now().minusDays(5).plusHours(2))
                .eventId("event-1")
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();

        adminJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("admin-1")
                .claim("preferred_username", "admin")
                .claim("realm_access.roles", "MODERATOR_ADMIN")
                .build();

        superAdminJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("super-1")
                .claim("preferred_username", "superadmin")
                .claim("realm_access.roles", "SUPER_ADMIN")
                .build();

        userJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-1")
                .claim("preferred_username", "user")
                .claim("realm_access.roles", "USER")
                .build();

        moderatorJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("mod-1")
                .claim("preferred_username", "moderator")
                .claim("realm_access.roles", "MODERATOR_ADMIN")
                .build();
    }

    @Test
    @DisplayName("getAllArchives - without before parameter")
    void getAllArchives_NoBefore_ShouldReturnAll() {
        when(archiveRepository.findAllByOrderByStartTimeDesc(any(Pageable.class)))
                .thenReturn(List.of(testArchive));

        List<ArchiveDto> result = archiveService.getAllArchives(10, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Archive");
        verify(archiveRepository).findAllByOrderByStartTimeDesc(any(Pageable.class));
    }

    @Test
    @DisplayName("getAllArchives - with before parameter")
    void getAllArchives_WithBefore_ShouldFilter() {
        String before = "2025-01-01T00:00:00";
        when(archiveRepository.findByStartTimeBefore(eq(before), any(Pageable.class)))
                .thenReturn(List.of(testArchive));

        List<ArchiveDto> result = archiveService.getAllArchives(10, before);

        assertThat(result).hasSize(1);
        verify(archiveRepository).findByStartTimeBefore(eq(before), any(Pageable.class));
        verify(archiveRepository, never()).findAllByOrderByStartTimeDesc(any());
    }

    @Test
    @DisplayName("getAllArchives - empty before string returns all")
    void getAllArchives_EmptyBefore_ShouldReturnAll() {
        when(archiveRepository.findAllByOrderByStartTimeDesc(any(Pageable.class)))
                .thenReturn(List.of());

        List<ArchiveDto> result = archiveService.getAllArchives(10, "");

        assertThat(result).isEmpty();
        verify(archiveRepository).findAllByOrderByStartTimeDesc(any(Pageable.class));
    }

    @Test
    @DisplayName("getArchive - returns archive by id")
    void getArchive_WhenExists_ShouldReturn() {
        when(archiveRepository.findById("archive-1")).thenReturn(Optional.of(testArchive));

        ArchiveDto result = archiveService.getArchive("archive-1");

        assertThat(result.getId()).isEqualTo("archive-1");
        assertThat(result.getViewsCount()).isEqualTo(100);
        assertThat(result.getDuration()).isEqualTo(3600);
    }

    @Test
    @DisplayName("getArchive - not found throws exception")
    void getArchive_NotFound_ShouldThrow() {
        when(archiveRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> archiveService.getArchive("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Archive not found");
    }

    @Test
    @DisplayName("getArchivesByEvent - returns archives for event")
    void getArchivesByEvent_ShouldReturnList() {
        when(archiveRepository.findByEventId("event-1")).thenReturn(List.of(testArchive));

        List<ArchiveDto> result = archiveService.getArchivesByEvent("event-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEventId()).isEqualTo("event-1");
    }

    @Test
    @DisplayName("createArchive - should create and return archive")
    void createArchive_ShouldCreate() {
        ArchiveDto.CreateRequest request = ArchiveDto.CreateRequest.builder()
                .title("New Archive")
                .description("New desc")
                .videoUrl("https://cdn.example.com/new.mp4")
                .thumbnailUrl("https://cdn.example.com/new-thumb.jpg")
                .duration(7200)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(2))
                .eventId("event-2")
                .build();

        when(archiveRepository.save(any(Archive.class))).thenAnswer(inv -> {
            Archive a = inv.getArgument(0);
            a.setId("new-archive-id");
            return a;
        });

        ArchiveDto result = archiveService.createArchive(request, adminJwt);

        assertThat(result.getTitle()).isEqualTo("New Archive");
        assertThat(result.getViewsCount()).isEqualTo(0);
        verify(archiveRepository).save(any(Archive.class));
    }

    @Test
    @DisplayName("updateArchive - admin can update")
    void updateArchive_ByAdmin_ShouldUpdate() {
        ArchiveDto.UpdateRequest request = ArchiveDto.UpdateRequest.builder()
                .title("Updated Title")
                .description("Updated desc")
                .build();

        when(archiveRepository.findById("archive-1")).thenReturn(Optional.of(testArchive));
        when(archiveRepository.save(any(Archive.class))).thenAnswer(inv -> inv.getArgument(0));

        ArchiveDto result = archiveService.updateArchive("archive-1", request, adminJwt);

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getDescription()).isEqualTo("Updated desc");
    }

    @Test
    @DisplayName("updateArchive - non-admin throws AccessDenied")
    void updateArchive_ByUser_ShouldThrow() {
        ArchiveDto.UpdateRequest request = ArchiveDto.UpdateRequest.builder()
                .title("Hacked")
                .build();

        when(archiveRepository.findById("archive-1")).thenReturn(Optional.of(testArchive));

        assertThatThrownBy(() -> archiveService.updateArchive("archive-1", request, userJwt))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only admin");
    }

    @Test
    @DisplayName("updateArchive - not found throws exception")
    void updateArchive_NotFound_ShouldThrow() {
        ArchiveDto.UpdateRequest request = ArchiveDto.UpdateRequest.builder().title("x").build();
        when(archiveRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> archiveService.updateArchive("nonexistent", request, adminJwt))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateArchive - partial update only changes provided fields")
    void updateArchive_PartialUpdate_ShouldOnlyChangeProvided() {
        ArchiveDto.UpdateRequest request = ArchiveDto.UpdateRequest.builder()
                .duration(9999)
                .build();

        when(archiveRepository.findById("archive-1")).thenReturn(Optional.of(testArchive));
        when(archiveRepository.save(any(Archive.class))).thenAnswer(inv -> inv.getArgument(0));

        ArchiveDto result = archiveService.updateArchive("archive-1", request, adminJwt);

        assertThat(result.getDuration()).isEqualTo(9999);
        assertThat(result.getTitle()).isEqualTo("Test Archive"); // unchanged
    }

    @Test
    @DisplayName("deleteArchive - superAdmin can delete any archive")
    void deleteArchive_BySuperAdmin_ShouldDelete() {
        when(archiveRepository.findById("archive-1")).thenReturn(Optional.of(testArchive));

        archiveService.deleteArchive("archive-1", superAdminJwt);

        verify(archiveRepository).deleteById("archive-1");
    }

    @Test
    @DisplayName("deleteArchive - non-admin throws AccessDenied")
    void deleteArchive_ByUser_ShouldThrow() {
        assertThatThrownBy(() -> archiveService.deleteArchive("archive-1", userJwt))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only admin");
    }

    @Test
    @DisplayName("deleteArchive - moderator cannot delete recent archive (< 30 days)")
    void deleteArchive_ModeratorRecentArchive_ShouldThrow() {
        testArchive.setCreatedAt(LocalDateTime.now().minusDays(10));
        when(archiveRepository.findById("archive-1")).thenReturn(Optional.of(testArchive));

        assertThatThrownBy(() -> archiveService.deleteArchive("archive-1", moderatorJwt))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("30 days");
    }

    @Test
    @DisplayName("deleteArchive - moderator can delete old archive (> 30 days)")
    void deleteArchive_ModeratorOldArchive_ShouldDelete() {
        testArchive.setCreatedAt(LocalDateTime.now().minusDays(60));
        when(archiveRepository.findById("archive-1")).thenReturn(Optional.of(testArchive));

        archiveService.deleteArchive("archive-1", moderatorJwt);

        verify(archiveRepository).deleteById("archive-1");
    }

    @Test
    @DisplayName("deleteArchive - not found throws IllegalArgumentException")
    void deleteArchive_NotFound_ShouldThrow() {
        when(archiveRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> archiveService.deleteArchive("nonexistent", superAdminJwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Archive not found");
    }

    @Test
    @DisplayName("incrementViews - delegates to repository")
    void incrementViews_ShouldDelegate() {
        doNothing().when(archiveRepository).incrementViews("archive-1");

        archiveService.incrementViews("archive-1");

        verify(archiveRepository).incrementViews("archive-1");
    }
}
