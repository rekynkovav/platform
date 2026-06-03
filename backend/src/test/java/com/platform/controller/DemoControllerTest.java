package com.platform.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DemoControllerTest {

    private DemoController demoController;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        demoController = new DemoController();
        jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-123")
                .build();
    }

    // ==================== Role ====================

    @Test
    void getDemoRole_ReturnsViewer() {
        ResponseEntity<Map<String, String>> response = demoController.getDemoRole(jwt);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("viewer", response.getBody().get("role"));
    }

    @Test
    void setDemoRole_SavesRole() {
        ResponseEntity<Map<String, Object>> response = demoController.setDemoRole(
                Map.of("role", "admin"), jwt);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(true, response.getBody().get("saved"));
        assertEquals("admin", response.getBody().get("role"));
    }

    @Test
    void setDemoRole_NoRole_DefaultsToViewer() {
        ResponseEntity<Map<String, Object>> response = demoController.setDemoRole(
                Map.of(), jwt);

        assertEquals("viewer", response.getBody().get("role"));
    }

    // ==================== Will Attend Counts ====================

    @Test
    void getWillAttendCounts_ReturnsEmptyMap() {
        ResponseEntity<Map<String, Integer>> response = demoController.getWillAttendCounts(jwt);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void saveWillAttendCounts_ReturnsSavedWithCount() {
        Map<String, Integer> counts = Map.of("event-1", 5, "event-2", 3);

        ResponseEntity<Map<String, Object>> response = demoController.saveWillAttendCounts(counts, jwt);

        assertEquals(true, response.getBody().get("saved"));
        assertEquals(2, response.getBody().get("entriesCount"));
    }

    // ==================== Attended ====================

    @Test
    void hasUserAttended_ReturnsFalse() {
        ResponseEntity<Map<String, Object>> response = demoController.hasUserAttended(
                "broadcast-1", "user-1", jwt);

        assertEquals(false, response.getBody().get("attended"));
        assertEquals("broadcast-1", response.getBody().get("broadcastId"));
    }

    @Test
    void markUserAttended_ReturnsTrue() {
        ResponseEntity<Map<String, Object>> response = demoController.markUserAttended(
                "broadcast-1", "user-1", jwt);

        assertEquals(true, response.getBody().get("attended"));
    }

    // ==================== Dashboard Menu ====================

    @Test
    void getDashboardMenuData_ReturnsEmptyList() {
        ResponseEntity<List<Map<String, Object>>> response = demoController.getDashboardMenuData(jwt);

        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void saveDashboardMenuData_ReturnsSavedWithItemCount() {
        List<Map<String, Object>> menuData = List.of(Map.of("item", "dashboard"));

        ResponseEntity<Map<String, Object>> response = demoController.saveDashboardMenuData(menuData, jwt);

        assertEquals(true, response.getBody().get("saved"));
        assertEquals(1, response.getBody().get("itemsCount"));
    }

    // ==================== Broadcasts ====================

    @Test
    void getBroadcasts_ReturnsEmptyList() {
        ResponseEntity<List<Map<String, Object>>> response = demoController.getBroadcasts(jwt);

        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void saveBroadcasts_ReturnsSavedWithCount() {
        List<Map<String, Object>> broadcasts = List.of(Map.of("id", "b1"), Map.of("id", "b2"));

        ResponseEntity<Map<String, Object>> response = demoController.saveBroadcasts(broadcasts, jwt);

        assertEquals(true, response.getBody().get("saved"));
        assertEquals(2, response.getBody().get("count"));
    }

    // ==================== Scheduled Events ====================

    @Test
    void getScheduledEvents_ReturnsEmptyList() {
        ResponseEntity<List<Map<String, Object>>> response = demoController.getScheduledEvents(jwt);

        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void saveScheduledEvents_ReturnsSavedWithCount() {
        List<Map<String, Object>> events = List.of(Map.of("id", "e1"));

        ResponseEntity<Map<String, Object>> response = demoController.saveScheduledEvents(events, jwt);

        assertEquals(true, response.getBody().get("saved"));
        assertEquals(1, response.getBody().get("count"));
    }
}
