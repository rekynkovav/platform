package com.platform.model.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserEventDtoTest {

    // ==================== UserDto ====================

    @Test
    void userDto_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();

        UserDto dto = UserDto.builder()
                .id("user-1")
                .username("alice")
                .email("alice@example.com")
                .phone("+79991234567")
                .role("USER")
                .avatarUrl("https://cdn.example.com/avatar.jpg")
                .bio("Hello world")
                .banned(false)
                .banReason(null)
                .lastActiveAt(now)
                .createdAt(now.minusDays(30))
                .build();

        assertEquals("user-1", dto.getId());
        assertEquals("alice", dto.getUsername());
        assertEquals("alice@example.com", dto.getEmail());
        assertEquals("+79991234567", dto.getPhone());
        assertEquals("USER", dto.getRole());
        assertFalse(dto.isBanned());
        assertNull(dto.getBanReason());
        assertEquals(now, dto.getLastActiveAt());
    }

    @Test
    void userDto_UpdateRequest_BuilderWorks() {
        UserDto.UpdateRequest req = UserDto.UpdateRequest.builder()
                .username("newname")
                .email("new@example.com")
                .phone("+1234567890")
                .avatarUrl("https://cdn.example.com/new.jpg")
                .bio("Updated bio")
                .build();

        assertEquals("newname", req.getUsername());
        assertEquals("new@example.com", req.getEmail());
        assertEquals("+1234567890", req.getPhone());
        assertEquals("Updated bio", req.getBio());
    }

    @Test
    void userDto_BannedUser_HasBanReason() {
        UserDto dto = UserDto.builder()
                .id("banned-user")
                .username("spammer")
                .banned(true)
                .banReason("Spam violation")
                .build();

        assertTrue(dto.isBanned());
        assertEquals("Spam violation", dto.getBanReason());
    }

    // ==================== EventDto ====================

    @Test
    void eventDto_Builder_SetsAllFields() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 15, 18, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 15, 20, 0);
        Map<String, String> links = Map.of("zoom", "https://zoom.us/j/123", "telegram", "https://t.me/event");

        EventDto dto = EventDto.builder()
                .id("event-1")
                .title("Summer Meetup")
                .description("Annual gathering")
                .startTime(start)
                .endTime(end)
                .coverImage("https://cdn.example.com/cover.jpg")
                .links(links)
                .streamUrl("https://stream.example.com/live")
                .status("UPCOMING")
                .createdBy("admin-1")
                .createdAt(LocalDateTime.now())
                .build();

        assertEquals("event-1", dto.getId());
        assertEquals("Summer Meetup", dto.getTitle());
        assertEquals(start, dto.getStartTime());
        assertEquals(end, dto.getEndTime());
        assertEquals(2, dto.getLinks().size());
        assertEquals("UPCOMING", dto.getStatus());
    }

    @Test
    void eventDto_CreateRequest_BuilderWorks() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 10, 0);

        EventDto.CreateRequest req = EventDto.CreateRequest.builder()
                .title("New Event")
                .description("Description")
                .startTime(start)
                .endTime(start.plusHours(2))
                .streamUrl("rtmp://live.example.com/stream")
                .build();

        assertEquals("New Event", req.getTitle());
        assertEquals(start, req.getStartTime());
        assertEquals("rtmp://live.example.com/stream", req.getStreamUrl());
    }

    @Test
    void eventDto_UpdateRequest_WithStatus_BuilderWorks() {
        EventDto.UpdateRequest req = EventDto.UpdateRequest.builder()
                .title("Updated Title")
                .status("CANCELLED")
                .build();

        assertEquals("Updated Title", req.getTitle());
        assertEquals("CANCELLED", req.getStatus());
        assertNull(req.getStartTime());
    }
}
