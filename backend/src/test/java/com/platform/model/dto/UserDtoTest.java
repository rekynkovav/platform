package com.platform.model.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserDtoTest {

    @Test
    void userDto_Builder_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        UserDto dto = UserDto.builder()
                .id("user-1")
                .username("alice")
                .email("alice@example.com")
                .phone("+79001234567")
                .role("USER_1")
                .avatarUrl("/avatars/alice.jpg")
                .bio("Hello, I'm Alice")
                .banned(false)
                .banReason(null)
                .lastActiveAt(now)
                .createdAt(now.minusDays(30))
                .build();

        assertEquals("user-1", dto.getId());
        assertEquals("alice", dto.getUsername());
        assertEquals("alice@example.com", dto.getEmail());
        assertEquals("+79001234567", dto.getPhone());
        assertEquals("USER_1", dto.getRole());
        assertFalse(dto.isBanned());
        assertNull(dto.getBanReason());
        assertEquals(now, dto.getLastActiveAt());
    }

    @Test
    void userDto_NoArgsConstructor_CreatesEmptyObject() {
        UserDto dto = new UserDto();
        assertNull(dto.getId());
        assertNull(dto.getUsername());
        assertFalse(dto.isBanned());
    }

    @Test
    void userDto_AllArgsConstructor_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        UserDto dto = new UserDto(
                "user-2", "bob", "bob@test.com", "+1234567890",
                "MODERATOR_ADMIN", "/avatars/bob.png", "Moderator",
                true, "Spam", now, now.minusDays(10)
        );

        assertEquals("user-2", dto.getId());
        assertEquals("bob", dto.getUsername());
        assertTrue(dto.isBanned());
        assertEquals("Spam", dto.getBanReason());
    }

    @Test
    void updateRequest_Builder_SetsAllFields() {
        UserDto.UpdateRequest req = UserDto.UpdateRequest.builder()
                .username("newname")
                .email("new@example.com")
                .phone("+79998887766")
                .avatarUrl("/avatars/new.jpg")
                .bio("Updated bio")
                .build();

        assertEquals("newname", req.getUsername());
        assertEquals("new@example.com", req.getEmail());
        assertEquals("+79998887766", req.getPhone());
        assertEquals("/avatars/new.jpg", req.getAvatarUrl());
        assertEquals("Updated bio", req.getBio());
    }

    @Test
    void updateRequest_SettersWork() {
        UserDto.UpdateRequest req = new UserDto.UpdateRequest();
        req.setUsername("setter_name");
        req.setEmail("setter@mail.com");
        req.setPhone("+1111111111");
        req.setAvatarUrl("/a/setter.jpg");
        req.setBio("Setter bio");

        assertEquals("setter_name", req.getUsername());
        assertEquals("setter@mail.com", req.getEmail());
        assertEquals("+1111111111", req.getPhone());
        assertEquals("/a/setter.jpg", req.getAvatarUrl());
        assertEquals("Setter bio", req.getBio());
    }
}
