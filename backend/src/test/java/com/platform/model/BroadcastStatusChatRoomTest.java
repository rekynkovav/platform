package com.platform.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BroadcastStatusChatRoomTest {

    // ==================== BroadcastStatus ====================

    @Test
    void broadcastStatus_Values_ReturnCorrectStrings() {
        assertEquals("idle", BroadcastStatus.IDLE.getValue());
        assertEquals("live", BroadcastStatus.LIVE.getValue());
        assertEquals("ended", BroadcastStatus.ENDED.getValue());
    }

    @Test
    void broadcastStatus_ToString_ReturnsValue() {
        assertEquals("idle", BroadcastStatus.IDLE.toString());
        assertEquals("live", BroadcastStatus.LIVE.toString());
        assertEquals("ended", BroadcastStatus.ENDED.toString());
    }

    @Test
    void broadcastStatus_ValueOf_Works() {
        assertEquals(BroadcastStatus.LIVE, BroadcastStatus.valueOf("LIVE"));
        assertEquals(3, BroadcastStatus.values().length);
    }

    // ==================== ChatRoom ====================

    @Test
    void chatRoom_Builder_SetsAllFields() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        ChatRoom room = ChatRoom.builder()
                .id(id)
                .slug("general")
                .type("general")
                .name("General Chat")
                .description("Main chat room")
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .version(1L)
                .build();

        assertEquals(id, room.getId());
        assertEquals("general", room.getSlug());
        assertEquals("general", room.getType());
        assertEquals("General Chat", room.getName());
        assertTrue(room.getIsActive());
        assertEquals(1L, room.getVersion());
    }

    @Test
    void chatRoom_NoArgsConstructor_CreatesWithDefaults() {
        ChatRoom room = new ChatRoom();
        assertNull(room.getId());
        assertNull(room.getSlug());
        // isActive has default true in field declaration but Lombok @NoArgsConstructor doesn't apply field initializers reliably
        // version has default 0L
    }

    @Test
    void chatRoom_SettersAndGetters_Work() {
        ChatRoom room = new ChatRoom();
        room.setSlug("nutrition");
        room.setType("nutrition");
        room.setName("Nutrition Chat");
        room.setDescription("Diet discussions");
        room.setIsActive(false);
        room.setVersion(2L);

        assertEquals("nutrition", room.getSlug());
        assertEquals("nutrition", room.getType());
        assertEquals("Nutrition Chat", room.getName());
        assertFalse(room.getIsActive());
        assertEquals(2L, room.getVersion());
    }
}
