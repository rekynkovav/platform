package com.platform.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {

    @Test
    void user_Builder_SetsAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        User user = User.builder()
                .id(id)
                .username("alice")
                .email("alice@example.com")
                .phone("+79001234567")
                .role("USER_1")
                .avatarUrl("/avatars/alice.jpg")
                .bio("Hello, I'm Alice")
                .lastActiveAt(now)
                .banned(false)
                .banReason(null)
                .bannedAt(null)
                .createdAt(now.minusDays(30))
                .updatedAt(now)
                .version(1L)
                .build();

        assertEquals(id, user.getId());
        assertEquals("alice", user.getUsername());
        assertEquals("alice@example.com", user.getEmail());
        assertEquals("+79001234567", user.getPhone());
        assertEquals("USER_1", user.getRole());
        assertFalse(user.isBanned());
        assertNull(user.getBanReason());
        assertEquals(1L, user.getVersion());
    }

    @Test
    void user_NoArgsConstructor_CreatesWithDefaults() {
        User user = new User();
        assertNull(user.getId());
        assertNull(user.getUsername());
        assertFalse(user.isBanned());
        // version has default 0L in field declaration
    }

    @Test
    void user_AllArgsConstructor_SetsAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        User user = new User(
                id, "bob", "bob@test.com", "+1234567890",
                "MODERATOR_ADMIN", "/avatars/bob.png", "Moderator",
                now, true, "Spam", now, now.minusDays(10), now, 2L
        );

        assertEquals(id, user.getId());
        assertEquals("bob", user.getUsername());
        assertTrue(user.isBanned());
        assertEquals("Spam", user.getBanReason());
        assertEquals(2L, user.getVersion());
    }

    @Test
    void user_BanFields_Work() {
        User user = new User();
        user.setBanned(true);
        user.setBanReason("Violation of rules");
        user.setBannedAt(LocalDateTime.now());

        assertTrue(user.isBanned());
        assertEquals("Violation of rules", user.getBanReason());
        assertNotNull(user.getBannedAt());
    }

    @Test
    void user_SettersAndGetters_Work() {
        User user = new User();
        user.setUsername("charlie");
        user.setEmail("charlie@mail.com");
        user.setPhone("+9999999999");
        user.setRole("CONSULTANT_PITANIE");
        user.setAvatarUrl("/avatars/c.jpg");
        user.setBio("Nutrition expert");
        user.setLastActiveAt(LocalDateTime.now());

        assertEquals("charlie", user.getUsername());
        assertEquals("charlie@mail.com", user.getEmail());
        assertEquals("CONSULTANT_PITANIE", user.getRole());
        assertEquals("Nutrition expert", user.getBio());
        assertNotNull(user.getLastActiveAt());
    }
}
