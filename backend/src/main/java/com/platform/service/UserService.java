package com.platform.service;

import com.platform.model.User;
import com.platform.model.dto.UserDto;
import com.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.dao.OptimisticLockingFailureException;
import io.micrometer.core.annotation.Timed;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Timed(value = "user.get_by_id", description = "Time to get user by ID")
    @Cacheable(value = "users", key = "#id")
    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    @Timed(value = "user.get_by_id_string", description = "Time to get user by ID string")
    @Cacheable(value = "users", key = "#id")
    public Optional<User> getUserById(String id) {
        return userRepository.findById(UUID.fromString(id));
    }

    @Timed(value = "user.update", description = "Time to update user")
    @CachePut(value = "users", key = "#user.id")
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Timed(value = "user.delete", description = "Time to delete user")
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }

    @Timed(value = "user.update_role", description = "Time to update user role")
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 1.5, maxDelay = 1000)
    )
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#userId"),
        @CacheEvict(value = "userRoles", key = "#userId")
    })
    public void updateUserRole(UUID userId, String role) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setRole(role);
        userRepository.save(user);
    }

    @Timed(value = "user.clear_cache", description = "Time to clear user cache")
    @CacheEvict(value = "users", allEntries = true)
    public void clearAllUserCache() {
        log.info("Cleared all user cache");
    }

    public String getUserName(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .map(User::getUsername)
                .orElse("User");
    }

    public boolean isAdmin(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .map(user -> {
                    String role = user.getRole();
                    return "MODERATOR_ADMIN".equals(role) || "SUPER_ADMIN".equals(role) || "VYACHESLAV".equals(role);
                })
                .orElse(false);
    }

    public boolean isModerator(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .map(user -> {
                    String role = user.getRole();
                    return "MODERATOR_ADMIN".equals(role) || "SUPER_ADMIN".equals(role) || "VYACHESLAV".equals(role);
                })
                .orElse(false);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public User syncUser(String userId, String username, String email, String role) {
        return userRepository.findById(UUID.fromString(userId))
                .map(user -> {
                    // Обновляем существующего
                    user.setUsername(username);
                    user.setEmail(email);
                    user.setRole(role);
                    user.setLastActiveAt(LocalDateTime.now());
                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    // Создаём нового
                    User newUser = User.builder()
                            .id(UUID.fromString(userId))
                            .username(username)
                            .email(email)
                            .role(role)
                            .lastActiveAt(LocalDateTime.now())
                            .createdAt(LocalDateTime.now())
                            .build();
                    return userRepository.save(newUser);
                });
    }

    @Transactional
    public void updateLastActive(String userId) {
        userRepository.findById(UUID.fromString(userId)).ifPresent(user -> {
            user.setLastActiveAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public Optional<User> updateUserProfile(String userId, UserDto.UpdateRequest request) {
        return userRepository.findById(UUID.fromString(userId))
                .map(user -> {
                    // Проверка уникальности username, если он изменяется
                    if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
                        if (userRepository.existsByUsername(request.getUsername())) {
                            throw new IllegalArgumentException("Username already taken");
                        }
                        user.setUsername(request.getUsername());
                    }

                    // Проверка уникальности email, если он изменяется
                    if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
                        userRepository.findByEmail(request.getEmail()).ifPresent(existingUser -> {
                            if (!existingUser.getId().equals(user.getId())) {
                                throw new IllegalArgumentException("Email already registered");
                            }
                        });
                        user.setEmail(request.getEmail());
                    }

                    if (request.getPhone() != null) user.setPhone(request.getPhone());
                    if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
                    if (request.getBio() != null) user.setBio(request.getBio());
                    user.setLastActiveAt(LocalDateTime.now());
                    return userRepository.save(user);
                });
    }

    @Timed(value = "user.find_by_id", description = "Time to find user by ID")
    @Cacheable(value = "users", key = "#userId")
    public Optional<User> findByUserId(String userId) {
        return userRepository.findById(UUID.fromString(userId));
    }
}
