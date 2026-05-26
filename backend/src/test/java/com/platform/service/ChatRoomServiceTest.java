package com.platform.service;

import com.platform.model.ChatRoom;
import com.platform.repository.ChatRoomRepository;
import com.platform.repository.ChatRoomPermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomPermissionRepository chatRoomPermissionRepository;

    @InjectMocks
    private ChatRoomService chatRoomService;

    @Test
    void testGetBySlug_WhenRoomExists() {
        String slug = "user1";
        ChatRoom mockRoom = ChatRoom.builder()
                .id(123L)
                .slug(slug)
                .name("Чат новичков")
                .type("general")
                .isActive(true)
                .build();

        when(chatRoomRepository.findBySlug(slug)).thenReturn(Optional.of(mockRoom));

        ChatRoom result = chatRoomService.getBySlug(slug);

        assertNotNull(result);
        assertEquals(slug, result.getSlug());
        assertEquals("Чат новичков", result.getName());
        verify(chatRoomRepository, times(1)).findBySlug(slug);
    }

    @Test
    void testGetBySlug_WhenRoomNotExists() {
        String slug = "non-existent";
        when(chatRoomRepository.findBySlug(slug)).thenReturn(Optional.empty());

        assertThrows(com.platform.exception.ResourceNotFoundException.class,
            () -> chatRoomService.getBySlug(slug));
        verify(chatRoomRepository, times(1)).findBySlug(slug);
    }

    @Test
    void testGetAllActiveRooms() {
        ChatRoom activeRoom = ChatRoom.builder()
                .id(1L)
                .slug("active")
                .name("Active Room")
                .type("general")
                .isActive(true)
                .build();

        ChatRoom inactiveRoom = ChatRoom.builder()
                .id(2L)
                .slug("inactive")
                .name("Inactive Room")
                .type("general")
                .isActive(false)
                .build();

        when(chatRoomRepository.findAll()).thenReturn(java.util.List.of(activeRoom, inactiveRoom));

        var result = chatRoomService.getAllActiveRooms();

        assertEquals(1, result.size());
        assertEquals("active", result.get(0).getSlug());
        verify(chatRoomRepository, times(1)).findAll();
    }
}
