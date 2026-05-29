package com.platform.service;

import com.platform.model.User;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .role("USER_1")
                .build();
    }

    @Test
    void testGetUserById_WithUuid_ReturnsUser() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.getUserById(testUserId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findById(testUserId);
    }

    @Test
    void testGetUserById_WithStringId_ReturnsUser() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.getUserById(testUserId.toString());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        verify(userRepository).findById(testUserId);
    }

    @Test
    void testGetUserById_WithInvalidStringId_ReturnsEmpty() {
        // Act
        Optional<User> result = userService.getUserById("invalid-uuid");

        // Assert
        assertThat(result).isEmpty();
        verify(userRepository, never()).findById(any());
    }

    @Test
    void testGetUserById_NonExistentUser_ReturnsEmpty() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.getUserById(testUserId);

        // Assert
        assertThat(result).isEmpty();
        verify(userRepository).findById(testUserId);
    }

    @Test
    void testUpdateUserRole_ValidRole_UpdatesRole() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act
        userService.updateUserRole(testUserId, "USER_2");

        // Assert
        assertThat(testUser.getRole()).isEqualTo("USER_2");
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateUserRole_InvalidRole_ThrowsException() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act & Assert
        try {
            userService.updateUserRole(testUserId, "INVALID_ROLE");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("Invalid role");
        }

        verify(userRepository, never()).save(any());
    }

    @Test
    void testUpdateUserRole_NonExistentUser_ThrowsException() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        try {
            userService.updateUserRole(testUserId, "USER_2");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("not found");
        }

        verify(userRepository, never()).save(any());
    }

    @Test
    void testGetUserByUsername_ReturnsUser() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.getUserByUsername("testuser");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void testGetUserByEmail_ReturnsUser() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.getUserByEmail("test@example.com");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void testCreateUser_SavesUser() {
        // Arrange
        User newUser = User.builder()
                .username("newuser")
                .email("new@example.com")
                .passwordHash("hashed-password")
                .role("USER_1")
                .build();

        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Act
        User result = userService.createUser(newUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("newuser");
        verify(userRepository).save(any(User.class));
    }
}