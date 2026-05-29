package com.platform.service;

import com.platform.model.ChatRoom;
import com.platform.model.Message;
import com.platform.model.dto.MessageDto;
import com.platform.repository.ChatRoomRepository;
import com.platform.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private UserService userService;

    @Mock
    private MediaFileRepository mediaFileRepository;

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private ChatRoomPermissionRepository chatRoomPermissionRepository;

    @InjectMocks
    private ChatService chatService;

    private ChatRoom testRoom;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        testRoom = ChatRoom.builder()
                .id("room-123")
                .slug("test-room")
                .name("Test Room")
                .build();

        testMessage = Message.builder()
                .id("msg-123")
                .roomId("room-123")
                .userId("user-123")
                .content("Test message")
                .build();
    }

    @Test
    void testGetMessagesByRoomWithPagination_ReturnsPagedMessages() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        MessageDto messageDto = MessageDto.builder()
                .id("msg-123")
                .content("Test message")
                .userId("user-123")
                .build();

        Page<MessageDto> expectedPage = new PageImpl<>(List.of(messageDto), pageable, 1);

        when(chatRoomRepository.findBySlug("test-room")).thenReturn(Optional.of(testRoom));
        when(messageRepository.findByRoomIdWithPagination(eq("room-123"), any(Pageable.class)))
                .thenReturn(expectedPage);

        // Act
        Page<MessageDto> result = chatService.getMessagesByRoomWithPagination("test-room", 0, 10, "USER_1");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("Test message");
        verify(messageRepository).findByRoomIdWithPagination(eq("room-123"), any(Pageable.class));
    }

    @Test
    void testGetMessagesByRoom_ReturnsLimitedMessages() {
        // Arrange
        Instant before = Instant.now();
        MessageDto messageDto = MessageDto.builder()
                .id("msg-123")
                .content("Test message")
                .userId("user-123")
                .build();

        when(chatRoomRepository.findBySlug("test-room")).thenReturn(Optional.of(testRoom));
        when(messageRepository.findByRoomIdBefore(eq("room-123"), eq(before), eq(20)))
                .thenReturn(List.of(messageDto));

        // Act
        List<MessageDto> result = chatService.getMessagesByRoom("test-room", 20, before, "USER_1");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Test message");
        verify(messageRepository).findByRoomIdBefore(eq("room-123"), eq(before), eq(20));
    }

    @Test
    void testSendMessageToRoom_ValidMessage_SavesAndReturnsDto() {
        // Arrange
        when(chatRoomRepository.findBySlug("test-room")).thenReturn(Optional.of(testRoom));
        when(chatRoomService.hasAccessToRoom("test-room", "user-123", "USER_1")).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        // Act
        MessageDto result = chatService.sendMessageToRoom("test-room", "user-123", "Hello world", null, "USER_1");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Test message");
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void testSendMessageToRoom_WithMedia_SavesWithMedia() {
        // Arrange
        when(chatRoomRepository.findBySlug("test-room")).thenReturn(Optional.of(testRoom));
        when(chatRoomService.hasAccessToRoom("test-room", "user-123", "USER_1")).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        // Act
        MessageDto result = chatService.sendMessageToRoom("test-room", "user-123", "Message with media", "media-123", "USER_1");

        // Assert
        assertThat(result).isNotNull();
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void testGetMessages_ReturnsPageOfMessages() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        MessageDto.ProjectionDto projectionDto = new MessageDto.ProjectionDto() {
            @Override public String getId() { return "msg-123"; }
            @Override public String getContent() { return "Test"; }
            @Override public String getUserId() { return "user-123"; }
            @Override public String getRoomId() { return "room-123"; }
        };

        Page<MessageDto.ProjectionDto> expectedPage = new PageImpl<>(List.of(projectionDto), pageable, 1);

        when(messageRepository.findByRoomId(eq("room-123"), any(Pageable.class)))
                .thenReturn(expectedPage);

        // Act
        Page<MessageDto.ProjectionDto> result = chatService.getMessages("room-123", 0, 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("Test");
        verify(messageRepository).findByRoomId(eq("room-123"), any(Pageable.class));
    }

    @Test
    void testGetMessagesAfter_ReturnsMessagesAfterCursor() {
        // Arrange
        MessageDto messageDto = MessageDto.builder()
                .id("msg-456")
                .content("New message")
                .userId("user-123")
                .build();

        when(messageRepository.findByRoomIdAndIdGreaterThan(eq("room-123"), eq("msg-123"), eq(50)))
                .thenReturn(List.of(messageDto));

        // Act
        List<MessageDto> result = chatService.getMessagesAfter("room-123", "msg-123", 50);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("New message");
        verify(messageRepository).findByRoomIdAndIdGreaterThan(eq("room-123"), eq("msg-123"), eq(50));
    }
}