package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.model.dto.MessageDto;
import com.platform.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    private String userId;
    private UUID userIdUuid;
    private String messageId;
    private UUID messageIdUuid;
    private String roomSlug;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatController).build();
        objectMapper = new ObjectMapper();

        userId = "user-123";
        userIdUuid = UUID.fromString(userId);
        messageId = "message-456";
        messageIdUuid = UUID.fromString(messageId);
        roomSlug = "general-chat";
    }

    @Test
    void sendToGeneralChat_ValidRequest_ReturnsMessageDto() throws Exception {
        // Arrange
        String content = "Hello world!";
        MessageDto expectedResponse = MessageDto.builder()
                .id(messageIdUuid)
                .content(content)
                .userId(userIdUuid)
                .userName("TestUser")
                .chatType("general")
                .createdAt(Instant.now())
                .build();

        when(chatService.sendToGeneralChat(eq(userId), eq(content), isNull()))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/chat/general")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + content + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(messageId))
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.chatType").value("general"));
    }

    @Test
    void sendToGeneralChat_WithMedia_ReturnsMessageDto() throws Exception {
        // Arrange
        String content = "Hello with media!";
        String mediaId = "media-789";
        MessageDto expectedResponse = MessageDto.builder()
                .id(messageIdUuid)
                .content(content)
                .userId(userIdUuid)
                .mediaId(UUID.fromString(mediaId))
                .mediaType("image/jpeg")
                .fileName("test.jpg")
                .chatType("general")
                .createdAt(Instant.now())
                .build();

        when(chatService.sendToGeneralChat(eq(userId), eq(content), eq(mediaId)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/chat/general")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + content + "\", \"mediaId\": \"" + mediaId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaId").value(mediaId))
                .andExpect(jsonPath("$.mediaType").value("image/jpeg"))
                .andExpect(jsonPath("$.fileName").value("test.jpg"));
    }

    @Test
    void sendToGeneralChat_MissingUserId_ReturnsBadRequest() throws Exception {
        // Arrange
        String content = "Hello world!";

        // Act & Assert
        mockMvc.perform(post("/api/chat/general")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + content + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendToGeneralChat_EmptyContent_ReturnsBadRequest() throws Exception {
        // Arrange
        String emptyContent = "";

        // Act & Assert
        mockMvc.perform(post("/api/chat/general")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + emptyContent + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendPrivateMessage_ValidRequest_ReturnsMessageDto() throws Exception {
        // Arrange
        String content = "Private message";
        String recipientId = "recipient-789";
        MessageDto expectedResponse = MessageDto.builder()
                .id(messageIdUuid)
                .content(content)
                .userId(userIdUuid)
                .recipientId(UUID.fromString(recipientId))
                .userName("TestUser")
                .chatType("private")
                .createdAt(Instant.now())
                .build();

        when(chatService.sendPrivateMessage(eq(userId), eq(recipientId), eq(content), isNull()))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/chat/private")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientId\": \"" + recipientId + "\", \"content\": \"" + content + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientId").value(recipientId))
                .andExpect(jsonPath("$.chatType").value("private"));
    }

    @Test
    void getMessagesByRoom_ValidRequest_ReturnsMessages() throws Exception {
        // Arrange
        MessageDto message1 = MessageDto.builder()
                .id(UUID.randomUUID())
                .content("Message 1")
                .userId(userIdUuid)
                .userName("User1")
                .roomSlug(roomSlug)
                .createdAt(Instant.now().minusSeconds(300))
                .build();

        MessageDto message2 = MessageDto.builder()
                .id(UUID.randomUUID())
                .content("Message 2")
                .userId(UUID.randomUUID())
                .userName("User2")
                .roomSlug(roomSlug)
                .createdAt(Instant.now().minusSeconds(150))
                .build();

        List<MessageDto> expectedResponse = List.of(message1, message2);

        when(chatService.getMessagesByRoom(eq(roomSlug), isNull(), isNull(), eq(userId)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/chat/room/{slug}", roomSlug)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].roomSlug").value(roomSlug))
                .andExpect(jsonPath("$[1].roomSlug").value(roomSlug));
    }

    @Test
    void getMessagesByRoom_WithPagination_ReturnsMessages() throws Exception {
        // Arrange
        String before = "2024-01-15T10:30:00Z";
        Integer limit = 50;

        MessageDto message = MessageDto.builder()
                .id(UUID.randomUUID())
                .content("Old message")
                .userId(userIdUuid)
                .userName("User1")
                .roomSlug(roomSlug)
                .createdAt(Instant.parse(before).minusSeconds(60))
                .build();

        List<MessageDto> expectedResponse = List.of(message);

        when(chatService.getMessagesByRoom(eq(roomSlug), eq(before), eq(limit), eq(userId)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/chat/room/{slug}", roomSlug)
                        .header("X-User-Id", userId)
                        .param("before", before)
                        .param("limit", limit.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getGeneralMessages_ValidRequest_ReturnsMessages() throws Exception {
        // Arrange
        String before = "2024-01-15T10:30:00Z";

        MessageDto message = MessageDto.builder()
                .id(UUID.randomUUID())
                .content("General message")
                .userId(userIdUuid)
                .userName("User1")
                .chatType("general")
                .createdAt(Instant.parse(before).minusSeconds(60))
                .build();

        List<MessageDto> expectedResponse = List.of(message);

        when(chatService.getGeneralMessages(eq(before)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/chat/general")
                        .param("before", before))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].chatType").value("general"));
    }

    @Test
    void markAsRead_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        when(chatService.markAsRead(eq(messageId), eq(userId))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/chat/messages/{id}/read", messageId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk());
    }

    @Test
    void markAsRead_MessageNotFound_ReturnsNotFound() throws Exception {
        // Arrange
        String nonExistentMessageId = "non-existent-message";
        when(chatService.markAsRead(eq(nonExistentMessageId), eq(userId)))
                .thenThrow(new com.platform.exception.ResourceNotFoundException("Message not found"));

        // Act & Assert
        mockMvc.perform(post("/api/chat/messages/{id}/read", nonExistentMessageId)
                        .header("X-User-Id", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUnreadCount_ValidRequest_ReturnsCount() throws Exception {
        // Arrange
        long unreadCount = 5L;
        when(chatService.getUnreadCount(eq(userId))).thenReturn(unreadCount);

        // Act & Assert
        mockMvc.perform(get("/api/chat/unread-count")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void getUnreadCount_MissingUserId_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/chat/unread-count"))
                .andExpect(status().isBadRequest());
    }
}