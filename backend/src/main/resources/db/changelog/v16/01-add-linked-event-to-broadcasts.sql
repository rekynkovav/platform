-- V16: Add linked_event_id to live_broadcasts for broadcast-event linking
ALTER TABLE live_broadcasts ADD COLUMN IF NOT EXISTS linked_event_id UUID;

CREATE INDEX IF NOT EXISTS idx_broadcast_linked_event ON live_broadcasts(linked_event_id);
