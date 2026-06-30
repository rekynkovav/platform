package com.platform.controller;

import com.platform.model.Notification;
import com.platform.service.NotificationService;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @RateLimiting(name = "notification-limit")
    public ResponseEntity<List<Notification>> getAll(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "50") int limit) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(notificationService.getAllForUser(userId, limit));
    }

    @GetMapping("/unread")
    @RateLimiting(name = "notification-limit")
    public ResponseEntity<List<Notification>> getUnread(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "50") int limit) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(notificationService.getUnreadForUser(userId, limit));
    }

    @PutMapping("/{id}/read")
    @RateLimiting(name = "notification-limit")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        notificationService.markAsReadForUser(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    @RateLimiting(name = "notification-limit")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Синхронизация уведомлений с фронтенда.
     * Принимает массив уведомлений и обновляет их статус read на сервере.
     * Frontend: notificationApi.updateNotifications(notifs)
     */
    @PostMapping("/sync")
    @RateLimiting(name = "notification-limit")
    public ResponseEntity<Map<String, Object>> syncNotifications(
            @RequestBody List<Map<String, Object>> notifications,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.debug("Syncing {} notifications for user {}", notifications.size(), userId);

        int updated = 0;
        for (Map<String, Object> notif : notifications) {
            try {
                Object idObj = notif.get("id");
                Boolean read = (Boolean) notif.get("read");

                // Обрабатываем только числовые ID (серверные уведомления)
                if (idObj instanceof Number && read != null) {
                    Long id = ((Number) idObj).longValue();
                    if (read) {
                        notificationService.markAsReadForUser(id, userId);
                        updated++;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to sync notification: {}", e.getMessage());
            }
        }

        log.debug("Synced {} notifications for user {}", updated, userId);
        return ResponseEntity.ok(Map.of(
                "synced", updated,
                "total", notifications.size()
        ));
    }
}
