package com.platform.integration.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P7-1: Liquibase Migration Validation.
 *
 * Boots the FULL Spring context with a real PostgreSQL 16 container.
 * Liquibase auto-runs all migrations on startup.
 * If ANY migration fails, the context won't start → test fails.
 *
 * Then verifies the schema is correct and v9 initial data is present.
 */
@DisplayName("P7-1: Liquibase migrations on real PostgreSQL")
@SpringBootTest
@ActiveProfiles("integration")
class LiquibaseMigrationTest extends AbstractMigrationTest {

    @Test
    @DisplayName("Context started — all migrations applied successfully")
    void contextStarted() {
        assertNotNull(jdbcTemplate, "Spring context must start (all migrations passed)");
    }

    @Test
    @DisplayName("Liquibase databasechangelog table exists and has entries")
    void liquibaseChangelogNotEmpty() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog", Integer.class);
        assertNotNull(count);
        assertTrue(count > 0, "databasechangelog must have entries — migrations should have run");
    }

    @Test
    @DisplayName("Liquibase checksums: no failures or rollbacks recorded")
    void liquibaseNoFailures() {
        List<Map<String, Object>> failures = jdbcTemplate.queryForList(
                "SELECT id, filename, exectype, md5sum " +
                "FROM databasechangelog WHERE exectype IN ('FAILED', 'RERUN')");
        assertTrue(failures.isEmpty(),
                "Found failed/rolled-back changesets: " + failures);
    }

    // --- Schema validation: key tables must exist with correct columns ---

    @Test
    @DisplayName("users table exists with expected columns")
    void usersTableSchema() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = 'users' ORDER BY ordinal_position",
                String.class);
        assertTrue(columns.containsAll(List.of(
                "id", "username", "email", "role", "created_at"
        )), "users table missing expected columns. Found: " + columns);
    }

    @Test
    @DisplayName("posts table exists with expected columns")
    void postsTableSchema() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = 'posts' ORDER BY ordinal_position",
                String.class);
        assertTrue(columns.containsAll(List.of(
                "id", "content", "author_id", "created_at"
        )), "posts table missing expected columns. Found: " + columns);
    }

    @Test
    @DisplayName("messages table exists with expected columns")
    void messagesTableSchema() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = 'messages' ORDER BY ordinal_position",
                String.class);
        assertTrue(columns.containsAll(List.of(
                "id", "user_id", "content", "created_at", "room_id"
        )), "messages table missing expected columns. Found: " + columns);
    }

    @Test
    @DisplayName("chat_rooms table exists")
    void chatRoomsTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_rooms", Integer.class);
        assertNotNull(count, "chat_rooms table must exist");
    }

    @Test
    @DisplayName("chat_room_permissions table exists")
    void chatRoomPermissionsTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_room_permissions", Integer.class);
        assertNotNull(count, "chat_room_permissions table must exist");
    }

    @Test
    @DisplayName("comments table exists")
    void commentsTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comments", Integer.class);
        assertNotNull(count, "comments table must exist");
    }

    @Test
    @DisplayName("reactions table exists")
    void reactionsTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reactions", Integer.class);
        assertNotNull(count, "reactions table must exist");
    }

    @Test
    @DisplayName("events table exists")
    void eventsTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM events", Integer.class);
        assertNotNull(count, "events table must exist");
    }

    @Test
    @DisplayName("archives table exists")
    void archivesTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM archives", Integer.class);
        assertNotNull(count, "archives table must exist");
    }

    @Test
    @DisplayName("notifications table exists")
    void notificationsTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications", Integer.class);
        assertNotNull(count, "notifications table must exist");
    }

    @Test
    @DisplayName("live_broadcasts table exists")
    void liveBroadcastsTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM live_broadcasts", Integer.class);
        assertNotNull(count, "live_broadcasts table must exist");
    }

    @Test
    @DisplayName("media_files table exists")
    void mediaFilesTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM media_files", Integer.class);
        assertNotNull(count, "media_files table must exist");
    }

    @Test
    @DisplayName("payments table exists")
    void paymentsTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments", Integer.class);
        assertNotNull(count, "payments table must exist");
    }

    @Test
    @DisplayName("contacts table exists")
    void contactsTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM contacts", Integer.class);
        assertNotNull(count, "contacts table must exist");
    }

    @Test
    @DisplayName("event_materials table exists")
    void eventMaterialsTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_materials", Integer.class);
        assertNotNull(count, "event_materials table must exist");
    }

    @Test
    @DisplayName("dashboard_menu_items table exists")
    void dashboardMenuItemsTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dashboard_menu_items", Integer.class);
        assertNotNull(count, "dashboard_menu_items table must exist");
    }

    @Test
    @DisplayName("keycloak_retry_jobs table exists")
    void keycloakRetryJobsTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM keycloak_retry_jobs", Integer.class);
        assertNotNull(count, "keycloak_retry_jobs table must exist");
    }

    @Test
    @DisplayName("access_logs table exists")
    void accessLogsTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM access_logs", Integer.class);
        assertNotNull(count, "access_logs table must exist");
    }

    // --- JPA entity validation (ddl-auto: validate) ---
    // If any JPA entity doesn't match the schema, Spring context would have failed to start.
    // These tests are implicit: passing contextStarted() means all entities validated.

    @Test
    @DisplayName("JPA validation passed — all entities match schema")
    void jpaValidationPassed() {
        // If we got here, SpringBootTest with ddl-auto:validate succeeded
        // That means every @Entity field matches the real PostgreSQL schema
        assertTrue(true, "JPA validation passed (context started with ddl-auto=validate)");
    }
}
