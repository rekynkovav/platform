package com.platform.repository;

import com.platform.model.entity.User;
import com.platform.model.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        entityManager.clear();

        // Create test users
        testUser1 = User.builder()
                .id(UUID.randomUUID())
                .username("testuser1")
                .email("test1@example.com")
                .passwordHash("hashed_password_1")
                .role(UserRole.USER_1)
                .banned(false)
                .createdAt(Instant.now().minusDays(10))
                .lastLogin(Instant.now().minusHours(2))
                .build();

        testUser2 = User.builder()
                .id(UUID.randomUUID())
                .username("testuser2")
                .email("test2@example.com")
                .passwordHash("hashed_password_2")
                .role(UserRole.MODERATOR)
                .banned(false)
                .createdAt(Instant.now().minusDays(5))
                .lastLogin(Instant.now().minusHours(1))
                .build();

        testUser3 = User.builder()
                .id(UUID.randomUUID())
                .username("testuser3")
                .email("test3@example.com")
                .passwordHash("hashed_password_3")
                .role(UserRole.USER_1)
                .banned(true)
                .banReason("Violation of rules")
                .bannedAt(Instant.now().minusDays(1))
                .createdAt(Instant.now().minusDays(3))
                .lastLogin(Instant.now().minusDays(2))
                .build();

        // Persist users
        entityManager.persist(testUser1);
        entityManager.persist(testUser2);
        entityManager.persist(testUser3);
        entityManager.flush();
    }

    @Test
    void findByUsername_ExistingUser_ReturnsUser() {
        // Act
        Optional<User> foundUser = userRepository.findByUsername("testuser1");

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser1");
        assertThat(foundUser.get().getEmail()).isEqualTo("test1@example.com");
        assertThat(foundUser.get().getRole()).isEqualTo(UserRole.USER_1);
    }

    @Test
    void findByUsername_NonExistentUser_ReturnsEmpty() {
        // Act
        Optional<User> foundUser = userRepository.findByUsername("nonexistent");

        // Assert
        assertThat(foundUser).isEmpty();
    }

    @Test
    void findByEmail_ExistingUser_ReturnsUser() {
        // Act
        Optional<User> foundUser = userRepository.findByEmail("test2@example.com");

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test2@example.com");
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser2");
        assertThat(foundUser.get().getRole()).isEqualTo(UserRole.MODERATOR);
    }

    @Test
    void findByEmail_NonExistentEmail_ReturnsEmpty() {
        // Act
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertThat(foundUser).isEmpty();
    }

    @Test
    void existsByUsername_ExistingUsername_ReturnsTrue() {
        // Act
        boolean exists = userRepository.existsByUsername("testuser1");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void existsByUsername_NonExistentUsername_ReturnsFalse() {
        // Act
        boolean exists = userRepository.existsByUsername("nonexistent");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmail_ExistingEmail_ReturnsTrue() {
        // Act
        boolean exists = userRepository.existsByEmail("test1@example.com");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_NonExistentEmail_ReturnsFalse() {
        // Act
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void findByRole_ValidRole_ReturnsUsers() {
        // Act
        List<User> users = userRepository.findByRole(UserRole.USER_1);

        // Assert
        assertThat(users).hasSize(2); // testUser1 and testUser3
        assertThat(users).extracting(User::getUsername)
                .containsExactlyInAnyOrder("testuser1", "testuser3");
    }

    @Test
    void findByRole_NoUsersWithRole_ReturnsEmptyList() {
        // Act
        List<User> users = userRepository.findByRole(UserRole.SUPER_ADMIN);

        // Assert
        assertThat(users).isEmpty();
    }

    @Test
    void findByBannedTrue_ReturnsBannedUsers() {
        // Act
        List<User> bannedUsers = userRepository.findByBannedTrue();

        // Assert
        assertThat(bannedUsers).hasSize(1);
        assertThat(bannedUsers.get(0).getUsername()).isEqualTo("testuser3");
        assertThat(bannedUsers.get(0).getBanReason()).isEqualTo("Violation of rules");
        assertThat(bannedUsers.get(0).getBannedAt()).isNotNull();
    }

    @Test
    void findByBannedFalse_ReturnsNonBannedUsers() {
        // Act
        List<User> nonBannedUsers = userRepository.findByBannedFalse();

        // Assert
        assertThat(nonBannedUsers).hasSize(2);
        assertThat(nonBannedUsers).extracting(User::getUsername)
                .containsExactlyInAnyOrder("testuser1", "testuser2");
    }

    @Test
    void countByBannedTrue_ReturnsCount() {
        // Act
        long count = userRepository.countByBannedTrue();

        // Assert
        assertThat(count).isEqualTo(1);
    }

    @Test
    void countByBannedFalse_ReturnsCount() {
        // Act
        long count = userRepository.countByBannedFalse();

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    void countByRole_ValidRole_ReturnsCount() {
        // Act
        long count = userRepository.countByRole(UserRole.USER_1);

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    void updateLastLogin_ValidUser_UpdatesTimestamp() {
        // Arrange
        Instant newLastLogin = Instant.now();

        // Act
        int updatedCount = userRepository.updateLastLogin(testUser1.getId(), newLastLogin);
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(updatedCount).isEqualTo(1);
        User updatedUser = entityManager.find(User.class, testUser1.getId());
        assertThat(updatedUser.getLastLogin()).isEqualTo(newLastLogin);
    }

    @Test
    void updateLastLogin_NonExistentUser_ReturnsZero() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        Instant newLastLogin = Instant.now();

        // Act
        int updatedCount = userRepository.updateLastLogin(nonExistentId, newLastLogin);

        // Assert
        assertThat(updatedCount).isEqualTo(0);
    }

    @Test
    void updateBanStatus_BanUser_UpdatesStatus() {
        // Arrange
        String banReason = "New violation";
        Instant bannedAt = Instant.now();

        // Act
        int updatedCount = userRepository.updateBanStatus(testUser1.getId(), true, banReason, bannedAt);
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(updatedCount).isEqualTo(1);
        User updatedUser = entityManager.find(User.class, testUser1.getId());
        assertThat(updatedUser.isBanned()).isTrue();
        assertThat(updatedUser.getBanReason()).isEqualTo(banReason);
        assertThat(updatedUser.getBannedAt()).isEqualTo(bannedAt);
    }

    @Test
    void updateBanStatus_UnbanUser_UpdatesStatus() {
        // Arrange
        // Act
        int updatedCount = userRepository.updateBanStatus(testUser3.getId(), false, null, null);
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(updatedCount).isEqualTo(1);
        User updatedUser = entityManager.find(User.class, testUser3.getId());
        assertThat(updatedUser.isBanned()).isFalse();
        assertThat(updatedUser.getBanReason()).isNull();
        assertThat(updatedUser.getBannedAt()).isNull();
    }

    @Test
    void updateRole_ValidUser_UpdatesRole() {
        // Arrange
        UserRole newRole = UserRole.MODERATOR_ADMIN;

        // Act
        int updatedCount = userRepository.updateRole(testUser1.getId(), newRole);
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(updatedCount).isEqualTo(1);
        User updatedUser = entityManager.find(User.class, testUser1.getId());
        assertThat(updatedUser.getRole()).isEqualTo(newRole);
    }

    @Test
    void findAllByOrderByCreatedAtDesc_ReturnsSortedUsers() {
        // Act
        List<User> users = userRepository.findAllByOrderByCreatedAtDesc();

        // Assert
        assertThat(users).hasSize(3);
        // Should be sorted by createdAt descending
        assertThat(users.get(0).getUsername()).isEqualTo("testuser1"); // Created 10 days ago
        assertThat(users.get(1).getUsername()).isEqualTo("testuser2"); // Created 5 days ago
        assertThat(users.get(2).getUsername()).isEqualTo("testuser3"); // Created 3 days ago
    }

    @Test
    void findActiveUsers_ReturnsUsersWithRecentLogin() {
        // Arrange
        Instant cutoff = Instant.now().minusDays(7);

        // Act
        List<User> activeUsers = userRepository.findActiveUsers(cutoff);

        // Assert
        assertThat(activeUsers).hasSize(2); // testUser1 and testUser2
        assertThat(activeUsers).extracting(User::getUsername)
                .containsExactlyInAnyOrder("testuser1", "testuser2");
    }

    @Test
    void findInactiveUsers_ReturnsUsersWithOldLogin() {
        // Arrange
        Instant cutoff = Instant.now().minusDays(7);

        // Act
        List<User> inactiveUsers = userRepository.findInactiveUsers(cutoff);

        // Assert
        assertThat(inactiveUsers).hasSize(1); // testUser3
        assertThat(inactiveUsers.get(0).getUsername()).isEqualTo("testuser3");
    }

    @Test
    void deleteByUsername_ExistingUser_DeletesUser() {
        // Act
        long deletedCount = userRepository.deleteByUsername("testuser1");
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(deletedCount).isEqualTo(1);
        Optional<User> deletedUser = userRepository.findByUsername("testuser1");
        assertThat(deletedUser).isEmpty();
    }

    @Test
    void deleteByUsername_NonExistentUser_ReturnsZero() {
        // Act
        long deletedCount = userRepository.deleteByUsername("nonexistent");

        // Assert
        assertThat(deletedCount).isEqualTo(0);
    }
}