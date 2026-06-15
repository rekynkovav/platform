package com.platform.service;

import com.platform.model.DashboardMenuItem;
import com.platform.repository.DashboardMenuItemRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardMenuService {

    private final DashboardMenuItemRepository repository;

    /**
     * Return all menu data grouped by section_key.
     * Format: { "broadcasts": [...], "exercises": [...], ... }
     * Frontend: api.get('/dashboard/menu') → setMenuData(response)
     */
    @Transactional(readOnly = true)
    public Map<String, List<Map<String, Object>>> getAllMenuData() {
        List<DashboardMenuItem> allItems = repository.findAllByOrderBySectionKeyAscSortOrderAsc();

        return allItems.stream()
            .collect(Collectors.groupingBy(
                DashboardMenuItem::getSectionKey,
                LinkedHashMap::new,
                Collectors.mapping(this::toMap, Collectors.toList())
            ));
    }

    /**
     * Full replace of all menu data.
     * Accepts Map<sectionKey, List<itemMap>> from frontend.
     * Deletes everything and re-inserts — simple, atomic, safe for admin-only operation.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveAllMenuData(
            Map<String, List<Map<String, Object>>> menuData,
            UUID userId
    ) {
        int totalItems = 0;

        for (Map.Entry<String, List<Map<String, Object>>> entry : menuData.entrySet()) {
            String sectionKey = entry.getKey();
            List<Map<String, Object>> items = entry.getValue();

            // Clear section
            repository.deleteBySectionKey(sectionKey);

            // Re-insert
            int order = 0;
            for (Map<String, Object> itemMap : items) {
                DashboardMenuItem item = fromMap(itemMap, sectionKey, order++, userId);
                repository.save(item);
                totalItems++;
            }
        }

        log.info("Dashboard menu saved by user {}: {} sections, {} items",
                userId, menuData.size(), totalItems);

        return Map.of(
            "saved", true,
            "sectionsCount", menuData.size(),
            "itemsCount", totalItems
        );
    }

    // ---- Mapping helpers ----

    private Map<String, Object> toMap(DashboardMenuItem item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId().toString());
        map.put("title", item.getTitle());
        map.put("description", item.getDescription());
        map.put("mediaUrl", item.getMediaUrl());
        map.put("mediaType", item.getMediaType());
        map.put("createdAt", item.getCreatedAt() != null ? item.getCreatedAt().toString() : null);
        return map;
    }

    private DashboardMenuItem fromMap(Map<String, Object> map, String sectionKey, int order, UUID userId) {
        return DashboardMenuItem.builder()
            .sectionKey(sectionKey)
            .sortOrder(order)
            .title(getStr(map, "title", ""))
            .description(getStr(map, "description", ""))
            .mediaUrl(getStr(map, "mediaUrl", ""))
            .mediaType(getStr(map, "mediaType", "text"))
            .createdBy(userId)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    private String getStr(Map<String, Object> map, String key, String fallback) {
        Object val = map.get(key);
        return val != null ? val.toString() : fallback;
    }

    /**
     * Seed default data on first startup.
     * Only runs if table is empty (safe to call on every boot).
     */
    @PostConstruct
    @Transactional
    public void seedDefaultsIfNeeded() {
        if (repository.count() > 0) {
            return; // Already seeded
        }

        log.info("Dashboard menu table is empty — seeding defaults from initialData.js");

        // Minimal set of defaults matching frontend initialData.js
        // Full initial data lives on the frontend; backend only needs enough
        // so the dashboard isn't blank before first admin save.
        String[][] sections = {
            {"broadcasts",          "Эфир #1: Основы здорового питания",      "audio"},
            {"broadcasts-guests",   "Эфир с гостем #1: Здоровое питание",     "audio"},
            {"exercises",           "Упражнение #1: Приседания",             "video"},
            {"recommendations",     "Рекомендация #1: Питьевой режим",        "text"},
            {"hardening",           "Закаливание #1: Контрастный душ",       "video"},
            {"about",               "О проекте: Путь Сердца",                "text"},
            {"newbies",             "Для новичков: Первые шаги",             "text"},
            {"heart-voice",         "Важные материалы: Голос сердца",        "text"},
            {"reflection",          "Для размышления: О осознанности",       "text"},
            {"interviews",          "Интервью #1: Интересные личности",      "audio"},
            {"participants",        "От участников: История успеха",         "video"},
            {"motivation",          "Мотивация: Начни сегодня",              "text"},
            {"reviews",             "Отзывы: Реальные результаты",           "text"},
            {"music",               "Музыка: Для медитации",                "audio"},
            {"movies",              "Фильмы: Документальные",                "video"},
            {"literature",          "Литература: Книги для развития",        "text"},
            {"about-timoshenko",    "О Тимошенко: Основатель",               "text"},
            {"challenge",           "Годовой челлендж: Физические нагрузки", "text"},
        };

        int order = 0;
        for (String[] s : sections) {
            repository.save(DashboardMenuItem.builder()
                .sectionKey(s[0])
                .sortOrder(order++)
                .title(s[1])
                .description("")
                .mediaType(s[2])
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        }

        log.info("Seeded {} default dashboard menu items", sections.length);
    }
}
