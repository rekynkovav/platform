-- V17: Add missing @Version columns to all entities that need optimistic locking
-- v6/06 only added version to: payments, users, live_broadcasts, messages
-- This migration adds version to the remaining 10 tables:
--   archives, events, event_materials, posts, notifications,
--   reactions, chat_rooms, media_files, contacts, comments

ALTER TABLE archives ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE events ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE event_materials ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE posts ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE reactions ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE chat_rooms ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE media_files ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE comments ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
