package com.platform.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChatRoomPermissionModelTest {

    @Test
    void chatRoomPermission_Builder_SetsAllFields() {
        Instant now = Instant.now();
        ChatRoom room = ChatRoom.builder()
                .id(UUID.randomUUID())
                .slug("test-room")
                .type("general")
                .name("Test Room")
                .isActive(true)
                .build();

        ChatRoomPermission perm = ChatRoomPermission.builder()
                .id(1L)
                .chatRoom(room)
                .role("ROLE_CONSULTANT_PITANIE")
                .permissionLevel("read_write")
                .createdAt(now)
                .build();

        assertEquals(1L, perm.getId());
        assertSame(room, perm.getChatRoom());
        assertEquals("ROLE_CONSULTANT_PITANIE", perm.getRole());
        assertEquals("read_write", perm.getPermissionLevel());
        assertEquals(now, perm.getCreatedAt());
    }

    @Test
    void chatRoomPermission_DefaultConstructor_CreatesInstance() {
        ChatRoomPermission perm = new ChatRoomPermission();
        assertNull(perm.getId());
        assertNull(perm.getChatRoom());
        assertNull(perm.getRole());
        assertNull(perm.getPermissionLevel());
    }

    @Test
    void chatRoomPermission_SettersAndGettersWork() {
        ChatRoomPermission perm = new ChatRoomPermission();
        perm.setRole("ROLE_USER");
        perm.setPermissionLevel("read");

        assertEquals("ROLE_USER", perm.getRole());
        assertEquals("read", perm.getPermissionLevel());
    }

    @Test
    void chatRoomPermission_AdminLevel_WorksCorrectly() {
        ChatRoomPermission perm = ChatRoomPermission.builder()
                .role("ROLE_VYACHESLAV")
                .permissionLevel("admin")
                .build();

        assertEquals("admin", perm.getPermissionLevel());
        assertEquals("ROLE_VYACHESLAV", perm.getRole());
    }
}
