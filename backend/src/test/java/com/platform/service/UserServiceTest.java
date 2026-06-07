package com.platform.service;

import com.platform.model.User;
import com.platform.model.dto.UserDto;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .phone("+79001234567")
                .role("USER")
                .avatarUrl("https://example.com/avatar.png")
                .bio("Test bio")
                .banned(false)
                .lastActiveAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("getUserById(UUID) - should return user when exists")
    void getUserById_Uuid_WhenExists_ShouldReturnUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.getUserById(userId);

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("getUserById(UUID) - should return empty when not exists")
    void getUserById_Uuid_WhenNotExists_ShouldReturnEmpty() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Optional<User> result = userService.getUserById(userId);

        assertThat(result).isEmpty();
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("getUserById(String) - should parse string UUID and return user")
    void getUserById_String_ShouldParseAndReturnUser() {
        String idStr = userId.toString();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.getUserById(idStr);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(userId);
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("updateUser - should save and return updated user")
    void updateUser_ShouldSaveAndReturn() {
        testUser.setUsername("updated");
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userService.updateUser(testUser);

        assertThat(result.getUsername()).isEqualTo("updated");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("deleteUser - should delete by id")
    void deleteUser_ShouldDeleteById() {
        doNothing().when(userRepository).deleteById(userId);

        userService.deleteUser(userId);

        verify(userRepository).deleteById(userId);
    }

    @Test
    @DisplayName("updateUserRole - should update role and save")
    void updateUserRole_ShouldUpdateAndSave() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUserRole(userId, "MODERATOR_ADMIN");

        assertThat(testUser.getRole()).isEqualTo("MODERATOR_ADMIN");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateUserRole - should throw when user not found")
    void updateUserRole_WhenNotFound_ShouldThrow() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserRole(userId, "ADMIN"))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    @DisplayName("getUserName - should return username when user exists")
    void getUserName_WhenExists_ShouldReturnUsername() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        String name = userService.getUserName(userId.toString());

        assertThat(name).isEqualTo("testuser");
    }

    @Test
    @DisplayName("getUserName - should return 'User' when user not found")
    void getUserName_WhenNotFound_ShouldReturnDefault() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        String name = userService.getUserName(userId.toString());

        assertThat(name).isEqualTo("User");
    }

    @Test
    @DisplayName("isAdmin - MODERATOR_ADMIN returns true")
    void isAdmin_ModeratorAdmin_ShouldReturnTrue() {
        testUser.setRole("MODERATOR_ADMIN");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThat(userService.isAdmin(userId.toString())).isTrue();
    }

    @Test
    @DisplayName("isAdmin - SUPER_ADMIN returns true")
    void isAdmin_SuperAdmin_ShouldReturnTrue() {
        testUser.setRole("SUPER_ADMIN");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThat(userService.isAdmin(userId.toString())).isTrue();
    }

    @Test
    @DisplayName("isAdmin - VYACHESLAV returns true")
    void isAdmin_Vyacheslav_ShouldReturnTrue() {
        testUser.setRole("VYACHESLAV");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThat(userService.isAdmin(userId.toString())).isTrue();
    }

    @Test
    @DisplayName("isAdmin - USER returns false")
    void isAdmin_User_ShouldReturnFalse() {
        testUser.setRole("USER");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThat(userService.isAdmin(userId.toString())).isFalse();
    }

    @Test
    @DisplayName("isAdmin - null role returns false")
    void isAdmin_NullRole_ShouldReturnFalse() {
        testUser.setRole(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThat(userService.isAdmin(userId.toString())).isFalse();
    }

    @Test
    @DisplayName("isAdmin - not found returns false")
    void isAdmin_NotFound_ShouldReturnFalse() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThat(userService.isAdmin(userId.toString())).isFalse();
    }

    @Test
    @DisplayName("isModerator - same logic as isAdmin")
    void isModerator_ModeratorAdmin_ShouldReturnTrue() {
        testUser.setRole("MODERATOR_ADMIN");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThat(userService.isModerator(userId.toString())).isTrue();
    }

    @Test
    @DisplayName("syncUser - existing user should be updated")
    void syncUser_Existing_ShouldUpdate() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.syncUser(userId.toString(), "newname", "new@mail.com", "ADMIN");

        assertThat(result.getUsername()).isEqualTo("newname");
        assertThat(result.getEmail()).isEqualTo("new@mail.com");
        assertThat(result.getRole()).isEqualTo("ADMIN");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("syncUser - new user should be created")
    void syncUser_New_ShouldCreate() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.syncUser(userId.toString(), "newuser", "new@mail.com", "USER");

        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getId()).isEqualTo(userId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateLastActive - should update timestamp when user exists")
    void updateLastActive_WhenExists_ShouldUpdate() {
        LocalDateTime before = LocalDateTime.now();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateLastActive(userId.toString());

        assertThat(testUser.getLastActiveAt()).isAfterOrEqualTo(before);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateLastActive - should do nothing when user not found")
    void updateLastActive_WhenNotFound_ShouldDoNothing() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        userService.updateLastActive(userId.toString());

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserProfile - update username successfully")
    void updateUserProfile_UpdateUsername_Success() {
        UserDto.UpdateRequest request = UserDto.UpdateRequest.builder()
                .username("newusername")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("newusername")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<User> result = userService.updateUserProfile(userId.toString(), request);

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("newusername");
    }

    @Test
    @DisplayName("updateUserProfile - duplicate username throws exception")
    void updateUserProfile_DuplicateUsername_ShouldThrow() {
        UserDto.UpdateRequest request = UserDto.UpdateRequest.builder()
                .username("existinguser")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUserProfile(userId.toString(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    @DisplayName("updateUserProfile - duplicate email throws exception")
    void updateUserProfile_DuplicateEmail_ShouldThrow() {
        UserDto.UpdateRequest request = UserDto.UpdateRequest.builder()
                .email("taken@example.com")
                .build();

        User otherUser = User.builder().id(UUID.randomUUID()).email("taken@example.com").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> userService.updateUserProfile(userId.toString(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    @DisplayName("updateUserProfile - same email for same user should not throw")
    void updateUserProfile_SameEmailSameUser_ShouldNotThrow() {
        UserDto.UpdateRequest request = UserDto.UpdateRequest.builder()
                .email("test@example.com")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<User> result = userService.updateUserProfile(userId.toString(), request);

        assertThat(result).isPresent();
        // findByEmail не должен вызываться, т.к. email не изменился
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("updateUserProfile - update all fields")
    void updateUserProfile_AllFields_ShouldUpdate() {
        UserDto.UpdateRequest request = UserDto.UpdateRequest.builder()
                .username("newname")
                .email("new@example.com")
                .phone("+79998887766")
                .avatarUrl("https://new.avatar/url.png")
                .bio("New bio text")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("newname")).thenReturn(false);
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<User> result = userService.updateUserProfile(userId.toString(), request);

        assertThat(result).isPresent();
        User u = result.get();
        assertThat(u.getUsername()).isEqualTo("newname");
        assertThat(u.getEmail()).isEqualTo("new@example.com");
        assertThat(u.getPhone()).isEqualTo("+79998887766");
        assertThat(u.getAvatarUrl()).isEqualTo("https://new.avatar/url.png");
        assertThat(u.getBio()).isEqualTo("New bio text");
    }

    @Test
    @DisplayName("updateUserProfile - user not found returns empty")
    void updateUserProfile_NotFound_ShouldReturnEmpty() {
        UserDto.UpdateRequest request = UserDto.UpdateRequest.builder().username("x").build();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Optional<User> result = userService.updateUserProfile(userId.toString(), request);

        assertThat(result).isEmpty();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("findByUserId - delegates to repository")
    void findByUserId_ShouldDelegateToRepo() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByUserId(userId.toString());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("clearAllUserCache - should execute without error")
    void clearAllUserCache_ShouldExecute() {
        // Метод только логирует, проверяем что не падает
        userService.clearAllUserCache();
        // No exception = success
    }
}
