package com.platform.controller;

import com.platform.service.DashboardMenuService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dashboard menu CRUD for admin panel.
 * Frontend calls:
 *   GET  /api/dashboard/menu     → Map<sectionKey, List<item>>
 *   POST /api/dashboard/menu     → full replace (admin only)
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardMenuService menuService;

    /**
     * Get all dashboard menu data grouped by section.
     * Returns: { "broadcasts": [...], "exercises": [...], ... }
     */
    @GetMapping("/menu")
    @Timed(value = "dashboard.menu.get", description = "Time to load dashboard menu")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> getMenuData(
            @AuthenticationPrincipal Jwt jwt) {

        log.debug("Dashboard menu requested by user: {}", jwt.getSubject());
        Map<String, List<Map<String, Object>>> data = menuService.getAllMenuData();
        return ResponseEntity.ok(data);
    }

    /**
     * Save all dashboard menu data (full replace).
     * Accepts: { "broadcasts": [...], "exercises": [...], ... }
     * Admin/Moderator only.
     */
    @PostMapping("/menu")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    @Timed(value = "dashboard.menu.save", description = "Time to save dashboard menu")
    @Counted(value = "dashboard.menu.save.count", description = "Dashboard menu saves")
    public ResponseEntity<Map<String, Object>> saveMenuData(
            @RequestBody Map<String, List<Map<String, Object>>> menuData,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Dashboard menu save by user {}: {} sections",
                userId, menuData.size());

        Map<String, Object> result = menuService.saveAllMenuData(menuData, userId);
        return ResponseEntity.ok(result);
    }
}
