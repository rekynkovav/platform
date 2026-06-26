package com.platform.controller;

import com.platform.model.dto.UserDto;
import com.platform.service.UserService;
import com.platform.enums.UserRole;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.UUID;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Получить текущего пользователя.
     * Если пользователя нет в БД — создаётся из JWT (auto-provision).
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        String role = extractPrimaryRole(jwt);

        // auto-provision: syncUser создаст юзера если его нет в БД
        var user = userService.syncUser(userId, username, email, role);
        return ResponseEntity.ok(UserDto.builder()
                .id(user.getId() != null ? user.getId().toString() : null)
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .banned(user.isBanned())
                .banReason(user.getBanReason())
                .lastActiveAt(user.getLastActiveAt())
                .createdAt(user.getCreatedAt())
                .build());
    }

    /**
     * Извлечь приоритетную роль из JWT realm_access.
     */
    private String extractPrimaryRole(Jwt jwt) {
        @SuppressWarnings("unchecked")
        List<String> roles = jwt.getClaimAsStringList("realm_access.roles");
        if (roles != null) {
            for (UserRole ur : UserRole.values()) {
                if (roles.contains(ur.name())) {
                    return ur.name();
                }
            }
        }
        return UserRole.USER_0.name();
    }

    /**
     * Обновить профиль текущего пользователя
     */
    @PutMapping("/me")
    @RateLimiting(name = "user-limit")
    public ResponseEntity<UserDto> updateCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserDto.UpdateRequest request) {
        String userId = jwt.getSubject();
        return userService.updateUserProfile(userId, request)
                .map(user -> ResponseEntity.ok(UserDto.builder()
                        .id(user.getId() != null ? user.getId().toString() : null)
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole())
                        .avatarUrl(user.getAvatarUrl())
                        .bio(user.getBio())
                        .lastActiveAt(user.getLastActiveAt())
                        .createdAt(user.getCreatedAt())
                        .build()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Получить пользователя по ID (публичный профиль)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable String userId) {
        return userService.getUserById(userId)
                .map(user -> ResponseEntity.ok(UserDto.builder()
                        .id(user.getId() != null ? user.getId().toString() : null)
                        .username(user.getUsername())
                        .role(user.getRole())
                        .avatarUrl(user.getAvatarUrl())
                        .bio(user.getBio())
                        .createdAt(user.getCreatedAt())
                        .build()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
