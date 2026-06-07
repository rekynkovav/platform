package com.platform.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Демо-контроллер для фронтенда.
 * Все эндпоинты являются заглушками, возвращающими пустые/дефолтные данные.
 *
 * Frontend вызывает 12 эндпоинтов:
 *   GET/POST /api/demo/role
 *   GET/POST /api/demo/will-attend-counts
 *   GET/POST /api/demo/attended/{broadcastId}/{userId}
 *   GET/POST /api/demo/dashboard-menu
 *   GET/POST /api/demo/broadcasts
 *   GET/POST /api/demo/scheduled-events
 */
@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    // === Role ===

    @GetMapping("/role")
    public ResponseEntity<Map<String, String>> getDemoRole(@AuthenticationPrincipal Jwt jwt) {
        log.debug("Getting demo role for user: {}", jwt.getSubject());
        return ResponseEntity.ok(Map.of("role", "viewer"));
    }

    @PostMapping("/role")
    public ResponseEntity<Map<String, Object>> setDemoRole(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        String role = body.getOrDefault("role", "viewer");
        log.info("Setting demo role '{}' for user: {}", role, jwt.getSubject());
        return ResponseEntity.ok(Map.of("saved", true, "role", role));
    }

    // === Will Attend Counts ===

    @GetMapping("/will-attend-counts")
    public ResponseEntity<Map<String, Integer>> getWillAttendCounts(@AuthenticationPrincipal Jwt jwt) {
        log.debug("Getting will-attend counts for user: {}", jwt.getSubject());
        return ResponseEntity.ok(Map.of());
    }

    @PostMapping("/will-attend-counts")
    public ResponseEntity<Map<String, Object>> saveWillAttendCounts(
            @RequestBody Map<String, Integer> counts,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Saving will-attend counts by user: {}, entries: {}", jwt.getSubject(), counts.size());
        return ResponseEntity.ok(Map.of("saved", true, "entriesCount", counts.size()));
    }

    // === Attended ===

    @GetMapping("/attended/{broadcastId}/{userId}")
    public ResponseEntity<Map<String, Object>> hasUserAttended(
            @PathVariable String broadcastId,
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt) {
        log.debug("Checking attendance: broadcast={}, user={}", broadcastId, userId);
        return ResponseEntity.ok(Map.of(
                "broadcastId", broadcastId,
                "userId", userId,
                "attended", false
        ));
    }

    @PostMapping("/attended/{broadcastId}/{userId}")
    public ResponseEntity<Map<String, Object>> markUserAttended(
            @PathVariable String broadcastId,
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Marking user {} as attended broadcast {}", userId, broadcastId);
        return ResponseEntity.ok(Map.of(
                "broadcastId", broadcastId,
                "userId", userId,
                "attended", true
        ));
    }

    // === Dashboard Menu ===

    @GetMapping("/dashboard-menu")
    public ResponseEntity<List<Map<String, Object>>> getDashboardMenuData(@AuthenticationPrincipal Jwt jwt) {
        log.debug("Getting demo dashboard menu for user: {}", jwt.getSubject());
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/dashboard-menu")
    public ResponseEntity<Map<String, Object>> saveDashboardMenuData(
            @RequestBody List<Map<String, Object>> menuData,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Saving demo dashboard menu by user: {}, items: {}", jwt.getSubject(), menuData.size());
        return ResponseEntity.ok(Map.of("saved", true, "itemsCount", menuData.size()));
    }

    // === Broadcasts ===

    @GetMapping("/broadcasts")
    public ResponseEntity<List<Map<String, Object>>> getBroadcasts(@AuthenticationPrincipal Jwt jwt) {
        log.debug("Getting demo broadcasts for user: {}", jwt.getSubject());
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/broadcasts")
    public ResponseEntity<Map<String, Object>> saveBroadcasts(
            @RequestBody List<Map<String, Object>> broadcasts,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Saving demo broadcasts by user: {}, count: {}", jwt.getSubject(), broadcasts.size());
        return ResponseEntity.ok(Map.of("saved", true, "count", broadcasts.size()));
    }

    // === Scheduled Events ===

    @GetMapping("/scheduled-events")
    public ResponseEntity<List<Map<String, Object>>> getScheduledEvents(@AuthenticationPrincipal Jwt jwt) {
        log.debug("Getting demo scheduled events for user: {}", jwt.getSubject());
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/scheduled-events")
    public ResponseEntity<Map<String, Object>> saveScheduledEvents(
            @RequestBody List<Map<String, Object>> events,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Saving demo scheduled events by user: {}, count: {}", jwt.getSubject(), events.size());
        return ResponseEntity.ok(Map.of("saved", true, "count", events.size()));
    }
}
