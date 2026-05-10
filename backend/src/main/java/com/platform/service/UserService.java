package com.platform.service;

import com.platform.model.User;
import com.platform.model.dto.UserDto;
import com.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public Optional<User> getUserById(String userId) {
        return userRepository.findById(userId);
    }

    public String getUserName(String userId) {
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElse("User");
    }

    public boolean isAdmin(String userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    String role = user.getRole();
                    return "MODERATOR_ADMIN".equals(role) || "SUPER_ADMIN".equals(role) || "VYACHESLAV".equals(role);
                })
                .orElse(false);
    }

    public boolean isModerator(String userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    String role = user.getRole();
                    return "MODERATOR_ADMIN".equals(role) || "SUPER_ADMIN".equals(role) || "VYACHESLAV".equals(role);
                })
                .orElse(false);
    }

    @Transactional
    public User syncUser(String userId, String username, String email, String role) {
        return userRepository.findById(userId)
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
                            .id(userId)
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
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastActiveAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    @Transactional
    public Optional<User> updateUserProfile(String userId, UserDto.UpdateRequest request) {
        return userRepository.findById(userId)
                .map(user -> {
                    if (request.getUsername() != null) user.setUsername(request.getUsername());
                    if (request.getEmail() != null) user.setEmail(request.getEmail());
                    if (request.getPhone() != null) user.setPhone(request.getPhone());
                    if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
                    if (request.getBio() != null) user.setBio(request.getBio());
                    user.setLastActiveAt(LocalDateTime.now());
                    return userRepository.save(user);
                });
    }
}
