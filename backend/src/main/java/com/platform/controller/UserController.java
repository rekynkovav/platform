package com.platform.controller;

import com.platform.model.dto.UserDto;
import com.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Получить текущего пользователя
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return userService.getUserById(userId)
                .map(user -> ResponseEntity.ok(UserDto.builder()
                        .id(user.getId())
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
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Обновить профиль текущего пользователя
     */
    @PutMapping("/me")
    public ResponseEntity<UserDto> updateCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserDto.UpdateRequest request) {

        String userId = jwt.getSubject();
        return userService.updateUserProfile(userId, request)
                .map(user -> ResponseEntity.ok(UserDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole())
                        .avatarUrl(user.getAvatarUrl())
                        .bio(user.getBio())
                        .lastActiveAt(user.getLastActiveAt())
                        .createdAt(user.getCreatedAt())
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Получить пользователя по ID (публичный профиль)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable String userId) {
        return userService.getUserById(userId)
                .map(user -> ResponseEntity.ok(UserDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .role(user.getRole())
                        .avatarUrl(user.getAvatarUrl())
                        .bio(user.getBio())
                        .createdAt(user.getCreatedAt())
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }
}
