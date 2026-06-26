package com.platform.controller;

import com.platform.model.dto.AdminStatsDto;
import com.platform.model.dto.UserDto;
import com.platform.service.AdminService;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.getAllUsers(search, pageable));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    public ResponseEntity<UserDto> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(adminService.getUserById(userId));
    }

    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'VYACHESLAV')")
    @RateLimiting(name = "admin-limit")
    public ResponseEntity<UserDto> updateUserRole(
            @PathVariable String userId,
            @RequestParam String role,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(adminService.updateUserRole(userId, role, jwt));
    }

    @PostMapping("/users/{userId}/ban")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    @RateLimiting(name = "admin-limit")
    public ResponseEntity<Void> banUser(
            @PathVariable String userId,
            @RequestParam String reason,
            @AuthenticationPrincipal Jwt jwt) {

        adminService.banUser(userId, reason, jwt);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{userId}/unban")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    @RateLimiting(name = "admin-limit")
    public ResponseEntity<Void> unbanUser(
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt) {

        adminService.unbanUser(userId, jwt);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts/flagged")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    public ResponseEntity<?> getFlaggedPosts() {
        return ResponseEntity.ok(adminService.getFlaggedPosts());
    }

    @PostMapping("/posts/{postId}/moderate")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    @RateLimiting(name = "admin-limit")
    public ResponseEntity<Void> moderatePost(
            @PathVariable String postId,
            @RequestParam boolean approve,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal Jwt jwt) {

        adminService.moderatePost(postId, approve, reason, jwt);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    public ResponseEntity<AdminStatsDto> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }
}
