package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.ChatRoom;
import com.platform.model.ChatRoomPermission;
import com.platform.model.MediaFile;
import com.platform.model.Message;
import com.platform.model.dto.MessageDto;
import com.platform.repository.ChatRoomPermissionRepository;
import com.platform.repository.ChatRoomRepository;
import com.platform.repository.MediaFileRepository;
import com.platform.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserService userService;

    @Mock
    private MediaFileRepository mediaFileRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private ChatRoomPermissionRepository chatRoomPermissionRepository;

    @InjectMocks
    private ChatService chatService;

    private String userId;
    private UUID userIdUuid;
    private String userName;
    private String roomSlug;

    @BeforeEach
    void setUp() {
        userId = "user-123";
        userIdUuid = UUID.fromString(userId);
        userName = "TestUser";
        roomSlug = "test-room";
    }

    @Test
    void sendToGeneralChat_SavesMessageCorrectly() {
        // Arrange
        String content = "Hello world!";
        when(userService.getUserName(userId)).thenReturn(userName);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message msg = inv.getArgument(0);
            msg.setId(UUID.randomUUID());
            return msg;
        });

        // Act
        MessageDto result = chatService.sendToGeneralChat(userId, content, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getUserName()).isEqualTo(userName);
        assertThat(result.getChatType()).isEqualTo("general");
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendToGeneralChat_WithMedia_SavesMessageWithMedia() {
        // Arrange
        String content = "Hello with media!";
        String mediaId = "media-456";
        UUID mediaIdUuid = UUID.fromString(mediaId);

        MediaFile mediaFile = MediaFile.builder()
                .id(mediaIdUuid)
                .fileName("test.jpg")
                .mediaType("image/jpeg")
                .build();

        when(userService.getUserName(userId)).thenReturn(userName);
        when(mediaFileRepository.findById(mediaIdUuid)).thenReturn(Optional.of(mediaFile));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message msg = inv.getArgument(0);
            msg.setId(UUID.randomUUID());
            return msg;
        });

        // Act
        MessageDto result = chatService.sendToGeneralChat(userId, content, mediaId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getMediaId()).isEqualTo(mediaIdUuid);
        assertThat(result.getMediaType()).isEqualTo("image/jpeg");
        assertThat(result.getFileName()).isEqualTo("test.jpg");
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendToGeneralChat_WithNonExistentMedia_ThrowsException() {
        // Arrange
        String content = "Hello with non-existent media!";
        String mediaId = "non-existent-media";
        UUID mediaIdUuid = UUID.fromString(mediaId);

        when(userService.getUserName(userId)).thenReturn(userName);
        when(mediaFileRepository.findById(mediaIdUuid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> chatService.sendToGeneralChat(userId, content, mediaId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Media file not found");

        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void sendPrivateMessage_SavesPrivateMessage() {
        // Arrange
        String content = "Private message";
        String recipientId = "recipient-789";
        UUID recipientIdUuid = UUID.fromString(recipientId);

        when(userService.getUserName(userId)).thenReturn(userName);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message msg = inv.getArgument(0);
            msg.setId(UUID.randomUUID());
            return msg;
        });

        // Act
        MessageDto result = chatService.sendPrivateMessage(userId, recipientId, content, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getRecipientId()).isEqualTo(recipientIdUuid);
        assertThat(result.getChatType()).isEqualTo("private");
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void markAsRead_ValidRecipient_MarksAsRead() {
        // Arrange
        String messageId = "message-123";
        UUID messageIdUuid = UUID.fromString(messageId);

        Message message = Message.builder()
                .id(messageIdUuid)
                .recipientId(userIdUuid)
                .isRead(false)
                .build();

        when(messageRepository.findById(messageIdUuid)).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        // Act
        chatService.markAsRead(messageId, userId);

        // Assert
        assertThat(message.isRead()).isTrue();
        verify(messageRepository).save(message);
    }

    @Test
    void markAsRead_WrongRecipient_DoesNotMarkAsRead() {
        // Arrange
        String messageId = "message-123";
        UUID messageIdUuid = UUID.fromString(messageId);
        String wrongUserId = "wrong-user-456";

        Message message = Message.builder()
                .id(messageIdUuid)
                .recipientId(userIdUuid)  // Original recipient
                .isRead(false)
                .build();

        when(messageRepository.findById(messageIdUuid)).thenReturn(Optional.of(message));

        // Act
        chatService.markAsRead(messageId, wrongUserId);

        // Assert
        assertThat(message.isRead()).isFalse();
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void markAsRead_MessageNotFound_ThrowsException() {
        // Arrange
        String messageId = "non-existent-message";
        UUID messageIdUuid = UUID.fromString(messageId);

        when(messageRepository.findById(messageIdUuid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> chatService.markAsRead(messageId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Message not found");

        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void getMessagesByRoom_HasPermission_ReturnsMessages() {
        // Arrange
        ChatRoom chatRoom = ChatRoom.builder()
                .id(UUID.randomUUID())
                .slug(roomSlug)
                .build();

        ChatRoomPermission permission = ChatRoomPermission.builder()
                .userId(userIdUuid)
                .chatRoom(chatRoom)
                .build();

        Message message = Message.builder()
                .id(UUID.randomUUID())
                .content("Test message")
                .userId(userIdUuid)
                .roomSlug(roomSlug)
                .build();

        when(chatRoomRepository.findBySlug(roomSlug)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionRepository.findByUserIdAndChatRoom(userIdUuid, chatRoom))
                .thenReturn(Optional.of(permission));
        when(messageRepository.findByRoomSlugOrderByCreatedAtDesc(roomSlug))
                .thenReturn(List.of(message));

        // Act
        List<MessageDto> result = chatService.getMessagesByRoom(roomSlug, null, null, userId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Test message");
        assertThat(result.get(0).getRoomSlug()).isEqualTo(roomSlug);
    }

    @Test
    void getMessagesByRoom_NoPermission_ThrowsException() {
        // Arrange
        ChatRoom chatRoom = ChatRoom.builder()
                .id(UUID.randomUUID())
                .slug(roomSlug)
                .build();

        when(chatRoomRepository.findBySlug(roomSlug)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionRepository.findByUserIdAndChatRoom(userIdUuid, chatRoom))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> chatService.getMessagesByRoom(roomSlug, null, null, userId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void getMessagesByRoom_RoomNotFound_ThrowsException() {
        // Arrange
        String nonExistentRoom = "non-existent-room";
        when(chatRoomRepository.findBySlug(nonExistentRoom)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> chatService.getMessagesByRoom(nonExistentRoom, null, null, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Chat room not found");
    }

    @Test
    void getUnreadCount_DelegatesToRepository() {
        // Arrange
        long expectedCount = 5L;
        when(messageRepository.countByRecipientIdAndIsRead(userIdUuid, false))
                .thenReturn(expectedCount);

        // Act
        long result = chatService.getUnreadCount(userId);

        // Assert
        assertThat(result).isEqualTo(expectedCount);
        verify(messageRepository).countByRecipientIdAndIsRead(userIdUuid, false);
    }

    @Test
    void getGeneralMessages_WithBefore_ParsesDateCorrectly() {
        // Arrange
        String before = "2024-01-15T10:30:00Z";
        Instant expectedInstant = Instant.parse(before);

        Message message = Message.builder()
                .id(UUID.randomUUID())
                .content("Old message")
                .chatType("general")
                .build();

        when(messageRepository.findByChatTypeAndCreatedAtBefore("general", expectedInstant))
                .thenReturn(List.of(message));

        // Act
        List<MessageDto> result = chatService.getGeneralMessages(before);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Old message");
        verify(messageRepository).findByChatTypeAndCreatedAtBefore("general", expectedInstant);
    }

    @Test
    void getGeneralMessages_InvalidDate_ThrowsException() {
        // Arrange
        String invalidDate = "not-a-date";

        // Act & Assert
        assertThatThrownBy(() -> chatService.getGeneralMessages(invalidDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid date format");

        verify(messageRepository, never()).findByChatTypeAndCreatedAtBefore(any(), any());
    }
}