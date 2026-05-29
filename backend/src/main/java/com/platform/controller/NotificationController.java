package com.platform.controller;

import com.platform.model.Notification;
import com.platform.service.NotificationService;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @RateLimiting(name = "notification-limit")
    public ResponseEntity<List<Notification>> getAll(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(notificationService.getAllForUser(userId));
    }

    @GetMapping("/unread")
    @RateLimiting(name = "notification-limit")
    public ResponseEntity<List<Notification>> getUnread(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(notificationService.getUnreadForUser(userId));
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
}
