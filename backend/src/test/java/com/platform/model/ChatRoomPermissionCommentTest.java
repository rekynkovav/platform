package com.platform.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChatRoomPermissionCommentTest {

    // ==================== ChatRoomPermission ====================

    @Test
    void chatRoomPermission_Builder_SetsAllFields() {
        ChatRoom room = ChatRoom.builder().id(UUID.randomUUID()).slug("test").type("general").name("Test").build();
        Instant now = Instant.now();

        ChatRoomPermission perm = ChatRoomPermission.builder()
                .id(1L)
                .chatRoom(room)
                .role("ROLE_USER")
                .permissionLevel("read")
                .createdAt(now)
                .build();

        assertEquals(1L, perm.getId());
        assertEquals(room, perm.getChatRoom());
        assertEquals("ROLE_USER", perm.getRole());
        assertEquals("read", perm.getPermissionLevel());
        assertEquals(now, perm.getCreatedAt());
    }

    @Test
    void chatRoomPermission_NoArgsConstructor_CreatesEmptyObject() {
        ChatRoomPermission perm = new ChatRoomPermission();
        assertNull(perm.getId());
        assertNull(perm.getChatRoom());
        assertNull(perm.getRole());
    }

    @Test
    void chatRoomPermission_SettersAndGetters_Work() {
        ChatRoomPermission perm = new ChatRoomPermission();
        ChatRoom room = ChatRoom.builder().slug("admin-room").type("admin").name("Admin").build();

        perm.setChatRoom(room);
        perm.setRole("MODERATOR_ADMIN");
        perm.setPermissionLevel("admin");

        assertEquals("admin-room", perm.getChatRoom().getSlug());
        assertEquals("MODERATOR_ADMIN", perm.getRole());
        assertEquals("admin", perm.getPermissionLevel());
    }

    // ==================== Comment ====================

    @Test
    void comment_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        Comment comment = Comment.builder()
                .id("cmt-1")
                .content("Great post!")
                .postId("post-1")
                .authorId("user-1")
                .authorName("Alice")
                .createdAt(now)
                .build();

        assertEquals("cmt-1", comment.getId());
        assertEquals("Great post!", comment.getContent());
        assertEquals("post-1", comment.getPostId());
        assertEquals("user-1", comment.getAuthorId());
        assertEquals("Alice", comment.getAuthorName());
        assertEquals(now, comment.getCreatedAt());
    }

    @Test
    void comment_NoArgsConstructor_CreatesEmptyObject() {
        Comment comment = new Comment();
        assertNull(comment.getId());
        assertNull(comment.getContent());
        assertNull(comment.getPostId());
    }
}
