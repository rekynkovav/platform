package com.platform.service;

import com.platform.model.User;
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
                .map(user -> "admin".equals(user.getRole()) || "moderator".equals(user.getRole()))
                .orElse(false);
    }

    public boolean isModerator(String userId) {
        return userRepository.findById(userId)
                .map(user -> "moderator".equals(user.getRole()))
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
}
