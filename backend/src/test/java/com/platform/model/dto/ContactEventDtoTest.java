package com.platform.model.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContactEventDtoTest {

    // ==================== ContactDto ====================

    @Test
    void contactDto_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        ContactDto dto = ContactDto.builder()
                .id("cnt-1")
                .userId("user-1")
                .contactUserId("user-2")
                .contactName("Bob")
                .status("accepted")
                .avatarUrl("/avatars/bob.jpg")
                .createdAt(now)
                .build();

        assertEquals("cnt-1", dto.getId());
        assertEquals("user-1", dto.getUserId());
        assertEquals("user-2", dto.getContactUserId());
        assertEquals("Bob", dto.getContactName());
        assertEquals("accepted", dto.getStatus());
        assertEquals("/avatars/bob.jpg", dto.getAvatarUrl());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    void contactDto_AddRequest_BuilderSetsFields() {
        ContactDto.AddRequest req = ContactDto.AddRequest.builder()
                .contactUserId("user-3")
                .nickname("Charlie")
                .build();

        assertEquals("user-3", req.getContactUserId());
        assertEquals("Charlie", req.getNickname());
    }

    @Test
    void contactDto_UpdateRequest_BuilderSetsNickname() {
        ContactDto.UpdateRequest req = ContactDto.UpdateRequest.builder()
                .nickname("NewNick")
                .build();

        assertEquals("NewNick", req.getNickname());
    }

    // ==================== EventDto ====================

    @Test
    void eventDto_Builder_SetsAllFields() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 18, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 1, 20, 0);
        Map<String, String> links = Map.of("telegram", "https://t.me/test");

        EventDto dto = EventDto.builder()
                .id("evt-1")
                .title("Summer Meetup")
                .description("Annual gathering")
                .startTime(start)
                .endTime(end)
                .coverImage("/covers/meetup.jpg")
                .links(links)
                .streamUrl("rtmp://live/stream")
                .status("scheduled")
                .createdBy("admin")
                .createdAt(LocalDateTime.now())
                .build();

        assertEquals("evt-1", dto.getId());
        assertEquals("Summer Meetup", dto.getTitle());
        assertEquals(start, dto.getStartTime());
        assertEquals(end, dto.getEndTime());
        assertEquals(links, dto.getLinks());
        assertEquals("scheduled", dto.getStatus());
    }

    @Test
    void eventDto_CreateRequest_BuilderSetsFields() {
        EventDto.CreateRequest req = EventDto.CreateRequest.builder()
                .title("New Event")
                .description("Desc")
                .streamUrl("rtmp://new")
                .build();

        assertEquals("New Event", req.getTitle());
        assertEquals("Desc", req.getDescription());
        assertEquals("rtmp://new", req.getStreamUrl());
    }

    @Test
    void eventDto_UpdateRequest_BuilderSetsFields() {
        EventDto.UpdateRequest req = EventDto.UpdateRequest.builder()
                .title("Updated")
                .status("cancelled")
                .build();

        assertEquals("Updated", req.getTitle());
        assertEquals("cancelled", req.getStatus());
    }
}
