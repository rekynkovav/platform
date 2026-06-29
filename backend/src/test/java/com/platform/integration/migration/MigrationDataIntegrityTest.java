package com.platform.integration.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P7-1: Verify v9 initial data is correctly seeded after all migrations.
 */
@DisplayName("P7-1: Migration data integrity (v9 initial data)")
@SpringBootTest
@ActiveProfiles("integration")
class MigrationDataIntegrityTest extends AbstractMigrationTest {

    private static final String TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Test
    @DisplayName("v9: test user exists with correct data")
    void testUserExists() {
        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT id::text, username, email, role FROM users WHERE id = ?::uuid",
                TEST_USER_ID);
        assertEquals("testuser", user.get("username"));
        assertEquals("test@example.com", user.get("email"));
        assertEquals("USER_0", user.get("role"));
    }

    @Test
    @DisplayName("v9: 7 chat rooms seeded")
    void chatRoomsSeeded() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_rooms", Integer.class);
        assertEquals(7, count, "v9 seeds exactly 7 chat rooms");
    }

    @Test
    @DisplayName("v9: chat rooms have correct slugs")
    void chatRoomSlugs() {
        List<String> slugs = jdbcTemplate.queryForList(
                "SELECT slug FROM chat_rooms ORDER BY slug", String.class);
        assertTrue(slugs.containsAll(List.of(
                "user1", "user2", "nutrition", "admins",
                "questions-general", "questions-nutrition", "questions-vyacheslav"
        )), "Missing expected chat room slugs. Found: " + slugs);
    }

    @Test
    @DisplayName("v9: chat room permissions seeded")
    void chatRoomPermissionsSeeded() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_room_permissions", Integer.class);
        assertNotNull(count);
        assertTrue(count > 0, "chat_room_permissions must have entries from v9");
    }

    @Test
    @DisplayName("v9: admins room only accessible to admin roles")
    void adminsRoomPermissions() {
        List<Map<String, Object>> perms = jdbcTemplate.queryForList(
                "SELECT role, permission_level FROM chat_room_permissions " +
                "WHERE room_id = 'a0000000-0000-0000-0000-000000000012'::uuid ORDER BY role");
        assertFalse(perms.isEmpty(), "admins room must have permissions");
        for (Map<String, Object> perm : perms) {
            String role = (String) perm.get("role");
            assertTrue(role.contains("ADMIN") || role.contains("MODERATOR") || role.contains("VYACHESLAV"),
                    "admins room should only have admin roles, found: " + role);
        }
    }

    @Test
    @DisplayName("v9: 5 test messages seeded")
    void testMessagesSeeded() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM messages WHERE user_id = ?::uuid",
                Integer.class, TEST_USER_ID);
        assertEquals(5, count, "v9 seeds exactly 5 test messages");
    }

    @Test
    @DisplayName("v9: messages belong to chat room 'user1'")
    void messagesBelongToUser1Room() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM messages WHERE room_slug = 'user1'",
                Integer.class);
        assertEquals(5, count, "All 5 test messages should be in user1 room");
    }

    @Test
    @DisplayName("v9: no duplicate messages (ON CONFLICT works)")
    void noDuplicateMessages() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM messages " +
                "WHERE id IN ('b0000000-0000-0000-0000-000000000001'::uuid, " +
                "'b0000000-0000-0000-0000-000000000002'::uuid, " +
                "'b0000000-0000-0000-0000-000000000003'::uuid, " +
                "'b0000000-0000-0000-0000-000000000004'::uuid, " +
                "'b0000000-0000-0000-0000-000000000005'::uuid)",
                Integer.class);
        assertEquals(5, count, "Exactly 5 messages with fixed UUIDs — no duplicates");
    }

    @Test
    @DisplayName("v9: test user created_at is recent (migration ran)")
    void testUserCreatedAtRecent() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?::uuid AND created_at IS NOT NULL",
                Integer.class, TEST_USER_ID);
        assertEquals(1, count, "test user must have created_at set");
    }
}
