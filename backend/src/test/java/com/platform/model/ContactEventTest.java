package com.platform.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContactEventTest {

    // ==================== Contact ====================

    @Test
    void contact_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        Contact contact = Contact.builder()
                .id("cnt-1")
                .userId("user-1")
                .contactUserId("user-2")
                .contactName("Bob")
                .createdAt(now)
                .updatedAt(now.plusMinutes(5))
                .build();

        assertEquals("cnt-1", contact.getId());
        assertEquals("user-1", contact.getUserId());
        assertEquals("user-2", contact.getContactUserId());
        assertEquals("Bob", contact.getContactName());
        assertEquals(now, contact.getCreatedAt());
    }

    @Test
    void contact_NoArgsConstructor_CreatesEmptyObject() {
        Contact contact = new Contact();
        assertNull(contact.getId());
        assertNull(contact.getUserId());
        assertNull(contact.getContactName());
    }

    @Test
    void contact_SettersAndGetters_Work() {
        Contact contact = new Contact();
        contact.setId("cnt-2");
        contact.setUserId("user-3");
        contact.setContactUserId("user-4");
        contact.setContactName("Charlie");

        assertEquals("cnt-2", contact.getId());
        assertEquals("user-3", contact.getUserId());
        assertEquals("Charlie", contact.getContactName());
    }

    // ==================== Event ====================

    @Test
    void event_Builder_SetsAllFields() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 18, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 1, 20, 0);
        Map<String, String> links = Map.of("telegram", "https://t.me/test");

        Event event = Event.builder()
                .id("evt-1")
                .title("Summer Meetup")
                .description("Annual gathering")
                .startTime(start)
                .endTime(end)
                .coverImage("/covers/meetup.jpg")
                .links(links)
                .createdBy("admin")
                .createdAt(LocalDateTime.now())
                .streamUrl("rtmp://live/stream")
                .status("scheduled")
                .build();

        assertEquals("evt-1", event.getId());
        assertEquals("Summer Meetup", event.getTitle());
        assertEquals(start, event.getStartTime());
        assertEquals(end, event.getEndTime());
        assertEquals(links, event.getLinks());
        assertEquals("scheduled", event.getStatus());
    }

    @Test
    void event_NoArgsConstructor_CreatesEmptyObject() {
        Event event = new Event();
        assertNull(event.getId());
        assertNull(event.getTitle());
        assertNull(event.getStartTime());
    }
}
