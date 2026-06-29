-- V18: Convert all VARCHAR(36) UUID columns to native uuid type
-- All v1 migrations used VARCHAR(36) for UUID columns, but Hibernate 6 expects native uuid type.
-- Order: users FIRST (referenced by FKs), then all other tables.

-- ============================================================
-- 1. users FIRST (referenced by FKs in live_broadcasts, etc.)
-- ============================================================
ALTER TABLE users ALTER COLUMN id TYPE uuid USING id::uuid;

-- ============================================================
-- 2. live_broadcasts (has FKs to users)
-- ============================================================
ALTER TABLE live_broadcasts DROP CONSTRAINT IF EXISTS fk_live_broadcasts_owner;
ALTER TABLE live_broadcasts DROP CONSTRAINT IF EXISTS fk_live_broadcasts_created_by;
ALTER TABLE live_broadcasts ALTER COLUMN id TYPE uuid USING id::uuid;
ALTER TABLE live_broadcasts ALTER COLUMN owner_id TYPE uuid USING owner_id::uuid;
ALTER TABLE live_broadcasts ALTER COLUMN created_by TYPE uuid USING created_by::uuid;
ALTER TABLE live_broadcasts ADD CONSTRAINT fk_live_broadcasts_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE live_broadcasts ADD CONSTRAINT fk_live_broadcasts_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

-- ============================================================
-- 3. posts (has FK to users via user_id)
-- ============================================================
ALTER TABLE posts ALTER COLUMN id TYPE uuid USING id::uuid;
ALTER TABLE posts ALTER COLUMN user_id TYPE uuid USING user_id::uuid;

-- ============================================================
-- 4. comments (has FKs to users, posts)
-- ============================================================
ALTER TABLE comments ALTER COLUMN id TYPE uuid USING id::uuid;
ALTER TABLE comments ALTER COLUMN user_id TYPE uuid USING user_id::uuid;
ALTER TABLE comments ALTER COLUMN post_id TYPE uuid USING post_id::uuid;

-- ============================================================
-- 5. reactions (has FKs to users, posts)
-- ============================================================
ALTER TABLE reactions ALTER COLUMN id TYPE uuid USING id::uuid;
ALTER TABLE reactions ALTER COLUMN user_id TYPE uuid USING user_id::uuid;
ALTER TABLE reactions ALTER COLUMN post_id TYPE uuid USING post_id::uuid;

-- ============================================================
-- 6. events (has FK to users via created_by)
-- ============================================================
ALTER TABLE events ALTER COLUMN id TYPE uuid USING id::uuid;
ALTER TABLE events ALTER COLUMN created_by TYPE uuid USING created_by::uuid;

-- ============================================================
-- 7. messages (has FK to users via user_id)
-- ============================================================
ALTER TABLE messages ALTER COLUMN id TYPE uuid USING id::uuid;
ALTER TABLE messages ALTER COLUMN user_id TYPE uuid USING user_id::uuid;
ALTER TABLE messages ALTER COLUMN recipient_id TYPE uuid USING recipient_id::uuid;

-- ============================================================
-- 8. archives
-- ============================================================
ALTER TABLE archives ALTER COLUMN id TYPE uuid USING id::uuid;
ALTER TABLE archives ALTER COLUMN event_id TYPE uuid USING event_id::uuid;

-- ============================================================
-- 9. media_files
-- ============================================================
ALTER TABLE media_files ALTER COLUMN id TYPE uuid USING id::uuid;
ALTER TABLE media_files ALTER COLUMN user_id TYPE uuid USING user_id::uuid;

-- ============================================================
-- 10. payments
-- ============================================================
ALTER TABLE payments ALTER COLUMN id TYPE uuid USING id::uuid;
ALTER TABLE payments ALTER COLUMN user_id TYPE uuid USING user_id::uuid;

-- ============================================================
-- 11. notifications
-- ============================================================
ALTER TABLE notifications ALTER COLUMN id TYPE uuid USING id::uuid;
ALTER TABLE notifications ALTER COLUMN recipient_id TYPE uuid USING recipient_id::uuid;

-- ============================================================
-- 12. chat_rooms
-- ============================================================
ALTER TABLE chat_rooms ALTER COLUMN id TYPE uuid USING id::uuid;

-- ============================================================
-- 13. chat_room_permissions
-- ============================================================
ALTER TABLE chat_room_permissions ALTER COLUMN room_id TYPE uuid USING room_id::uuid;

-- ============================================================
-- 14. contacts
-- ============================================================
ALTER TABLE contacts ALTER COLUMN id TYPE uuid USING id::uuid;
ALTER TABLE contacts ALTER COLUMN user_id TYPE uuid USING user_id::uuid;

-- ============================================================
-- 15. event_materials
-- ============================================================
ALTER TABLE event_materials ALTER COLUMN id TYPE uuid USING id::uuid;
ALTER TABLE event_materials ALTER COLUMN event_id TYPE uuid USING event_id::uuid;
