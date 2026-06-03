package com.platform.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class NotificationBroadcastStatusTest {

    // ==================== Notification ====================

    @Test
    void notification_Builder_SetsAllFields() {
        Instant now = Instant.now();
        Notification n = Notification.builder()
                .id(1L)
                .userId("user-1")
                .type("event")
                .title("New Event")
                .message("Event started")
                .relatedId(42L)
                .relatedRoomSlug("questions-general")
                .read(false)
                .createdAt(now)
                .build();

        assertEquals(1L, n.getId());
        assertEquals("user-1", n.getUserId());
        assertEquals("event", n.getType());
        assertEquals("New Event", n.getTitle());
        assertEquals("Event started", n.getMessage());
        assertEquals(42L, n.getRelatedId());
        assertEquals("questions-general", n.getRelatedRoomSlug());
        assertFalse(n.getRead());
        assertEquals(now, n.getCreatedAt());
    }

    @Test
    void notification_DefaultRead_IsFalse() {
        Notification n = new Notification();
        assertFalse(n.getRead());
    }

    @Test
    void notification_SettersAndGetters_Work() {
        Notification n = new Notification();
        n.setId(5L);
        n.setUserId("u2");
        n.setType("answer");
        n.setTitle("Answer received");
        n.setMessage("Your question was answered");
        n.setRelatedId(99L);
        n.setRelatedRoomSlug("questions-tech");
        n.setRead(true);

        assertEquals(5L, n.getId());
        assertEquals("u2", n.getUserId());
        assertEquals("answer", n.getType());
        assertTrue(n.getRead());
    }

    @Test
    void notification_NoArgConstructor_CreatesInstance() {
        Notification n = new Notification();
        assertNotNull(n);
    }

    // ==================== BroadcastStatus ====================

    @Test
    void broadcastStatus_Values_ReturnsThreeEntries() {
        assertEquals(3, BroadcastStatus.values().length);
    }

    @Test
    void broadcastStatus_GetValue_ReturnsCorrectStrings() {
        assertEquals("idle", BroadcastStatus.IDLE.getValue());
        assertEquals("live", BroadcastStatus.LIVE.getValue());
        assertEquals("ended", BroadcastStatus.ENDED.getValue());
    }

    @Test
    void broadcastStatus_ToString_ReturnsValue() {
        assertEquals("live", BroadcastStatus.LIVE.toString());
    }

    @Test
    void broadcastStatus_ValueOf_WorksForAllEntries() {
        assertEquals(BroadcastStatus.IDLE, BroadcastStatus.valueOf("IDLE"));
        assertEquals(BroadcastStatus.LIVE, BroadcastStatus.valueOf("LIVE"));
        assertEquals(BroadcastStatus.ENDED, BroadcastStatus.valueOf("ENDED"));
    }
}
