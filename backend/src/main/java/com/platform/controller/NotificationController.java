package com.platform.controller;

import com.platform.model.Notification;
import com.platform.service.NotificationService;
import com.github.vladimir_bukhtoyarov.bucket4j.spring.boot.starter.annotation.Bucket4j;
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
    @Bucket4j(name = "notification-api", bandwidths = {
        @Bucket4j.Bandwidth(limit = 30, duration = 1, unit = "MINUTES")
    })
    public ResponseEntity<List<Notification>> getAll(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(notificationService.getAllForUser(userId));
    }

    @GetMapping("/unread")
    @Bucket4j(name = "notification-api", bandwidths = {
        @Bucket4j.Bandwidth(limit = 30, duration = 1, unit = "MINUTES")
    })
    public ResponseEntity<List<Notification>> getUnread(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(notificationService.getUnreadForUser(userId));
    }

    @PutMapping("/{id}/read")
    @Bucket4j(name = "notification-api", bandwidths = {
        @Bucket4j.Bandwidth(limit = 30, duration = 1, unit = "MINUTES")
    })
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    @Bucket4j(name = "notification-api", bandwidths = {
        @Bucket4j.Bandwidth(limit = 30, duration = 1, unit = "MINUTES")
    })
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}