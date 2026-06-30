package com.platform.service;

import com.platform.model.DashboardMenuItem;
import com.platform.repository.DashboardMenuItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardMenuServiceTest {

    @Mock private DashboardMenuItemRepository repository;

    @InjectMocks
    private DashboardMenuService dashboardMenuService;

    private DashboardMenuItem item1;
    private DashboardMenuItem item2;

    @BeforeEach
    void setUp() {
        item1 = DashboardMenuItem.builder()
                .id(UUID.randomUUID())
                .sectionKey("broadcasts")
                .sortOrder(0)
                .title("Эфир #1")
                .description("Описание")
                .mediaUrl("https://example.com/audio.mp3")
                .mediaType("audio")
                .createdAt(LocalDateTime.now())
                .build();

        item2 = DashboardMenuItem.builder()
                .id(UUID.randomUUID())
                .sectionKey("broadcasts")
                .sortOrder(1)
                .title("Эфир #2")
                .description("Описание 2")
                .mediaType("audio")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== getAllMenuData ====================

    @Test
    void getAllMenuData_ReturnsGroupedBySection() {
        DashboardMenuItem exerciseItem = DashboardMenuItem.builder()
                .id(UUID.randomUUID())
                .sectionKey("exercises")
                .sortOrder(0)
                .title("Упражнение #1")
                .mediaType("video")
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.findAllByOrderBySectionKeyAscSortOrderAsc())
                .thenReturn(List.of(item1, item2, exerciseItem));

        Map<String, List<Map<String, Object>>> result = dashboardMenuService.getAllMenuData();

        assertThat(result).containsKeys("broadcasts", "exercises");
        assertThat(result.get("broadcasts")).hasSize(2);
        assertThat(result.get("exercises")).hasSize(1);
    }

    @Test
    void getAllMenuData_EmptyRepository_ReturnsEmptyMap() {
        when(repository.findAllByOrderBySectionKeyAscSortOrderAsc())
                .thenReturn(Collections.emptyList());

        Map<String, List<Map<String, Object>>> result = dashboardMenuService.getAllMenuData();

        assertThat(result).isEmpty();
    }

    // ==================== saveAllMenuData ====================

    @Test
    void saveAllMenuData_SavesItemsAndReturnsSummary() {
        UUID userId = UUID.randomUUID();
        Map<String, List<Map<String, Object>>> menuData = new LinkedHashMap<>();
        menuData.put("broadcasts", List.of(
                Map.of("title", "Эфир #1", "mediaType", "audio")
        ));

        when(repository.save(any(DashboardMenuItem.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = dashboardMenuService.saveAllMenuData(menuData, userId);

        assertThat(result.get("saved")).isEqualTo(true);
        assertThat(result.get("sectionsCount")).isEqualTo(1);
        assertThat(result.get("itemsCount")).isEqualTo(1);
        verify(repository).deleteBySectionKey("broadcasts");
        verify(repository).save(any(DashboardMenuItem.class));
    }

    @Test
    void saveAllMenuData_EmptyData_NoSaves() {
        UUID userId = UUID.randomUUID();
        Map<String, List<Map<String, Object>>> menuData = Map.of();

        Map<String, Object> result = dashboardMenuService.saveAllMenuData(menuData, userId);

        assertThat(result.get("sectionsCount")).isEqualTo(0);
        assertThat(result.get("itemsCount")).isEqualTo(0);
        verify(repository, never()).deleteBySectionKey(anyString());
        verify(repository, never()).save(any());
    }

    // ==================== seedDefaultsIfNeeded ====================

    @Test
    void seedDefaultsIfNeeded_TableNotEmpty_DoesNotSeed() {
        when(repository.count()).thenReturn(10L);

        dashboardMenuService.seedDefaultsIfNeeded();

        verify(repository, never()).save(any());
    }

    @Test
    void seedDefaultsIfNeeded_TableEmpty_SeedsDefaults() {
        when(repository.count()).thenReturn(0L);
        when(repository.save(any(DashboardMenuItem.class))).thenAnswer(inv -> inv.getArgument(0));

        dashboardMenuService.seedDefaultsIfNeeded();

        verify(repository, times(18)).save(any(DashboardMenuItem.class)); // 18 default sections
    }
}
