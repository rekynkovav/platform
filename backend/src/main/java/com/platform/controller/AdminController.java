package com.platform.controller;

import com.platform.model.dto.AdminStatsDto;
import com.platform.model.dto.UserDto;
import com.platform.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('admin', 'moderator', 'super_admin')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<List<UserDto>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {

        return ResponseEntity.ok(adminService.getAllUsers(search, limit, offset));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<UserDto> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(adminService.getUserById(userId));
    }

    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<UserDto> updateUserRole(
            @PathVariable String userId,
            @RequestParam String role,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(adminService.updateUserRole(userId, role, jwt));
    }

    @PostMapping("/users/{userId}/ban")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<Void> banUser(
            @PathVariable String userId,
            @RequestParam String reason,
            @AuthenticationPrincipal Jwt jwt) {

        adminService.banUser(userId, reason, jwt);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{userId}/unban")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<Void> unbanUser(
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt) {

        adminService.unbanUser(userId, jwt);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts/flagged")
    @PreAuthorize("hasAnyRole('admin', 'moderator')")
    public ResponseEntity<?> getFlaggedPosts() {
        return ResponseEntity.ok(adminService.getFlaggedPosts());
    }

    @PostMapping("/posts/{postId}/moderate")
    @PreAuthorize("hasAnyRole('admin', 'moderator')")
    public ResponseEntity<Void> moderatePost(
            @PathVariable String postId,
            @RequestParam boolean approve,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal Jwt jwt) {

        adminService.moderatePost(postId, approve, reason, jwt);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<AdminStatsDto> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }
}
