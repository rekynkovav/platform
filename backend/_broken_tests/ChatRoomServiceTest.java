package com.platform.service;

import com.platform.exception.ResourceNotFoundException;
import com.platform.model.ChatRoom;
import com.platform.model.ChatRoomPermission;
import com.platform.model.dto.ChatRoomDto;
import com.platform.repository.ChatRoomPermissionRepository;
import com.platform.repository.ChatRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomPermissionRepository chatRoomPermissionRepository;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private UUID roomId;
    private ChatRoom generalRoom;
    private ChatRoom nutritionRoom;
    private ChatRoom inactiveRoom;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();

        generalRoom = ChatRoom.builder()
                .id(roomId)
                .slug("questions-general")
                .type("question")
                .name("General Questions")
                .description("General Q&A")
                .isActive(true)
                .build();

        nutritionRoom = ChatRoom.builder()
                .id(UUID.randomUUID())
                .slug("questions-nutrition")
                .type("question")
                .name("Nutrition Questions")
                .description("Nutrition Q&A")
                .isActive(true)
                .build();

        inactiveRoom = ChatRoom.builder()
                .id(UUID.randomUUID())
                .slug("old-room")
                .type("chat")
                .name("Old Room")
                .isActive(false)
                .build();
    }

    // ==================== getBySlug / findBySlug ====================

    @Test
    void getBySlug_Exists_ReturnsRoom() {
        when(chatRoomRepository.findBySlug("questions-general")).thenReturn(Optional.of(generalRoom));

        ChatRoom result = chatRoomService.getBySlug("questions-general");

        assertThat(result).isNotNull();
        assertThat(result.getSlug()).isEqualTo("questions-general");
    }

    @Test
    void getBySlug_NotFound_ThrowsException() {
        when(chatRoomRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomService.getBySlug("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findBySlug_Exists_ReturnsRoom() {
        when(chatRoomRepository.findBySlug("questions-general")).thenReturn(Optional.of(generalRoom));

        ChatRoom result = chatRoomService.findBySlug("questions-general");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("General Questions");
    }

    @Test
    void findBySlug_NotFound_ThrowsException() {
        when(chatRoomRepository.findBySlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomService.findBySlug("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== getAllActiveRooms ====================

    @Test
    void getAllActiveRooms_ReturnsOnlyActive() {
        when(chatRoomRepository.findAll()).thenReturn(List.of(generalRoom, nutritionRoom, inactiveRoom));

        List<ChatRoom> result = chatRoomService.getAllActiveRooms();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(ChatRoom::getIsActive);
    }

    @Test
    void getAllActiveRooms_NoActive_ReturnsEmpty() {
        when(chatRoomRepository.findAll()).thenReturn(List.of(inactiveRoom));

        List<ChatRoom> result = chatRoomService.getAllActiveRooms();

        assertThat(result).isEmpty();
    }

    // ==================== getQuestionRooms ====================

    @Test
    void getQuestionRooms_ReturnsOnlyActiveQuestionRooms() {
        ChatRoom chatTypeRoom = ChatRoom.builder()
                .id(UUID.randomUUID()).slug("user1").type("chat").isActive(true).build();
        when(chatRoomRepository.findAll()).thenReturn(List.of(generalRoom, nutritionRoom, chatTypeRoom, inactiveRoom));

        List<ChatRoom> result = chatRoomService.getQuestionRooms();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> "question".equals(r.getType()));
    }

    // ==================== getQuestionRoomByMode ====================

    @Test
    void getQuestionRoomByMode_General_ReturnsGeneralRoom() {
        when(chatRoomRepository.findBySlug("questions-general")).thenReturn(Optional.of(generalRoom));

        ChatRoom result = chatRoomService.getQuestionRoomByMode("general");

        assertThat(result.getSlug()).isEqualTo("questions-general");
    }

    @Test
    void getQuestionRoomByMode_Nutrition_ReturnsNutritionRoom() {
        when(chatRoomRepository.findBySlug("questions-nutrition")).thenReturn(Optional.of(nutritionRoom));

        ChatRoom result = chatRoomService.getQuestionRoomByMode("nutrition");

        assertThat(result.getSlug()).isEqualTo("questions-nutrition");
    }

    @Test
    void getQuestionRoomByMode_Vyacheslav_ReturnsVyacheslavRoom() {
        ChatRoom vyacheslavRoom = ChatRoom.builder()
                .id(UUID.randomUUID()).slug("questions-vyacheslav").type("question").isActive(true).build();
        when(chatRoomRepository.findBySlug("questions-vyacheslav")).thenReturn(Optional.of(vyacheslavRoom));

        ChatRoom result = chatRoomService.getQuestionRoomByMode("vyacheslav");

        assertThat(result.getSlug()).isEqualTo("questions-vyacheslav");
    }

    @Test
    void getQuestionRoomByMode_Unknown_DefaultsToGeneral() {
        when(chatRoomRepository.findBySlug("questions-general")).thenReturn(Optional.of(generalRoom));

        ChatRoom result = chatRoomService.getQuestionRoomByMode("unknown-mode");

        assertThat(result.getSlug()).isEqualTo("questions-general");
    }

    // ==================== getQuestionRoomsDto ====================

    @Test
    void getQuestionRoomsDto_ReturnsMappedDtos() {
        when(chatRoomRepository.findAll()).thenReturn(List.of(generalRoom, nutritionRoom));

        List<ChatRoomDto> result = chatRoomService.getQuestionRoomsDto();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSlug()).isIn("questions-general", "questions-nutrition");
        assertThat(result.get(0).getType()).isEqualTo("question");
    }

    // ==================== setPermission ====================

    @Test
    void setPermission_ExistingPermission_UpdatesLevel() {
        ChatRoomPermission existing = ChatRoomPermission.builder()
                .chatRoom(generalRoom).role("ROLE_USER_0").permissionLevel("read").build();
        when(chatRoomPermissionRepository.findByChatRoomIdAndRole(roomId, "ROLE_USER_0"))
                .thenReturn(Optional.of(existing));
        when(chatRoomPermissionRepository.save(any(ChatRoomPermission.class))).thenAnswer(inv -> inv.getArgument(0));

        chatRoomService.setPermission(roomId, "ROLE_USER_0", "read_write");

        assertThat(existing.getPermissionLevel()).isEqualTo("read_write");
        verify(chatRoomPermissionRepository).save(existing);
    }

    @Test
    void setPermission_NewPermission_CreatesNew() {
        when(chatRoomPermissionRepository.findByChatRoomIdAndRole(roomId, "ROLE_NEW"))
                .thenReturn(Optional.empty());
        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(generalRoom));
        when(chatRoomPermissionRepository.save(any(ChatRoomPermission.class))).thenAnswer(inv -> inv.getArgument(0));

        chatRoomService.setPermission(roomId, "ROLE_NEW", "admin");

        verify(chatRoomPermissionRepository).save(argThat(p ->
                "ROLE_NEW".equals(p.getRole()) && "admin".equals(p.getPermissionLevel())));
    }

    // ==================== hasPermission ====================

    @Test
    void hasPermission_AdminLevel_ReturnsTrueForAnyRequired() {
        ChatRoomPermission perm = ChatRoomPermission.builder()
                .chatRoom(generalRoom).role("ROLE_ADMIN").permissionLevel("admin").build();
        when(chatRoomPermissionRepository.findByChatRoomIdAndRole(roomId, "ROLE_ADMIN"))
                .thenReturn(Optional.of(perm));

        assertThat(chatRoomService.hasPermission(roomId, "ROLE_ADMIN", "read")).isTrue();
        assertThat(chatRoomService.hasPermission(roomId, "ROLE_ADMIN", "read_write")).isTrue();
    }

    @Test
    void hasPermission_ReadWriteLevel_ReturnsTrueForReadWriteAndRead() {
        ChatRoomPermission perm = ChatRoomPermission.builder()
                .chatRoom(generalRoom).role("ROLE_USER").permissionLevel("read_write").build();
        when(chatRoomPermissionRepository.findByChatRoomIdAndRole(roomId, "ROLE_USER"))
                .thenReturn(Optional.of(perm));

        assertThat(chatRoomService.hasPermission(roomId, "ROLE_USER", "read_write")).isTrue();
        assertThat(chatRoomService.hasPermission(roomId, "ROLE_USER", "read")).isTrue();
    }

    @Test
    void hasPermission_ReadLevel_ReturnsTrueOnlyForRead() {
        ChatRoomPermission perm = ChatRoomPermission.builder()
                .chatRoom(generalRoom).role("ROLE_GUEST").permissionLevel("read").build();
        when(chatRoomPermissionRepository.findByChatRoomIdAndRole(roomId, "ROLE_GUEST"))
                .thenReturn(Optional.of(perm));

        assertThat(chatRoomService.hasPermission(roomId, "ROLE_GUEST", "read")).isTrue();
        assertThat(chatRoomService.hasPermission(roomId, "ROLE_GUEST", "read_write")).isFalse();
    }

    @Test
    void hasPermission_NoPermission_ReturnsFalse() {
        when(chatRoomPermissionRepository.findByChatRoomIdAndRole(roomId, "ROLE_UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThat(chatRoomService.hasPermission(roomId, "ROLE_UNKNOWN", "read")).isFalse();
    }

    @Test
    void hasPermission_NullRoomId_ReturnsFalse() {
        assertThat(chatRoomService.hasPermission(null, "ROLE_USER", "read")).isFalse();
    }

    @Test
    void hasPermission_NullRole_ReturnsFalse() {
        assertThat(chatRoomService.hasPermission(roomId, null, "read")).isFalse();
    }

    @Test
    void hasPermission_EmptyRole_ReturnsFalse() {
        assertThat(chatRoomService.hasPermission(roomId, "", "read")).isFalse();
    }
}
