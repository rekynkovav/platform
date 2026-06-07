package com.platform.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MessageContactModelTest {

    // ==================== Message ====================

    @Test
    void message_Builder_SetsAllFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Message msg = Message.builder()
                .id(id)
                .content("Hello!")
                .userId(userId)
                .userName("Alice")
                .chatType("private")
                .recipientId(recipientId)
                .isRead(false)
                .mediaId("media-1")
                .mediaType("image")
                .fileName("photo.jpg")
                .createdAt(now)
                .roomId(roomId)
                .roomSlug("questions-general")
                .version(0L)
                .build();

        assertEquals(id, msg.getId());
        assertEquals("Hello!", msg.getContent());
        assertEquals(userId, msg.getUserId());
        assertEquals("Alice", msg.getUserName());
        assertEquals("private", msg.getChatType());
        assertEquals(recipientId, msg.getRecipientId());
        assertFalse(msg.getIsRead());
        assertEquals("media-1", msg.getMediaId());
        assertEquals("image", msg.getMediaType());
        assertEquals("photo.jpg", msg.getFileName());
        assertEquals(roomId, msg.getRoomId());
        assertEquals("questions-general", msg.getRoomSlug());
    }

    @Test
    void message_DefaultConstructor_CreatesInstance() {
        Message msg = new Message();
        assertNull(msg.getId());
        assertNull(msg.getContent());
        assertFalse(msg.getIsRead());
        assertEquals(0L, msg.getVersion());
    }

    @Test
    void message_SettersAndGettersWork() {
        Message msg = new Message();
        msg.setContent("Updated");
        msg.setChatType("general");
        msg.setIsRead(true);
        msg.setRoomSlug("nutrition");

        assertEquals("Updated", msg.getContent());
        assertEquals("general", msg.getChatType());
        assertTrue(msg.getIsRead());
        assertEquals("nutrition", msg.getRoomSlug());
    }

    // ==================== Contact ====================

    @Test
    void contact_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();

        Contact contact = Contact.builder()
                .id("contact-1")
                .userId("user-1")
                .contactUserId("user-2")
                .contactName("Bob")
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals("contact-1", contact.getId());
        assertEquals("user-1", contact.getUserId());
        assertEquals("user-2", contact.getContactUserId());
        assertEquals("Bob", contact.getContactName());
        assertEquals(now, contact.getCreatedAt());
        assertEquals(now, contact.getUpdatedAt());
    }

    @Test
    void contact_DefaultConstructor_CreatesInstance() {
        Contact contact = new Contact();
        assertNull(contact.getId());
        assertNull(contact.getUserId());
        assertNull(contact.getContactName());
    }

    @Test
    void contact_SettersAndGettersWork() {
        Contact contact = new Contact();
        contact.setUserId("user-3");
        contact.setContactUserId("user-4");
        contact.setContactName("Charlie");

        assertEquals("user-3", contact.getUserId());
        assertEquals("user-4", contact.getContactUserId());
        assertEquals("Charlie", contact.getContactName());
    }
}
