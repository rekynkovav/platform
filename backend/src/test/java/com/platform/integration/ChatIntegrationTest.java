package com.platform.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.model.dto.MessageDto;
import com.platform.model.entity.User;
import com.platform.model.enums.ChatType;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;
    private String user1Id;
    private String user2Id;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser1 = User.builder()
                .id(UUID.randomUUID())
                .username("integration_user1")
                .email("integration1@example.com")
                .passwordHash("hashed_password_1")
                .createdAt(Instant.now())
                .build();

        testUser2 = User.builder()
                .id(UUID.randomUUID())
                .username("integration_user2")
                .email("integration2@example.com")
                .passwordHash("hashed_password_2")
                .createdAt(Instant.now())
                .build();

        userRepository.save(testUser1);
        userRepository.save(testUser2);

        user1Id = testUser1.getId().toString();
        user2Id = testUser2.getId().toString();
    }

    @Test
    void sendGeneralMessage_ValidRequest_ReturnsMessageDto() throws Exception {
        // Arrange
        String messageContent = "Hello everyone in general chat!";
        MessageDto request = MessageDto.builder()
                .content(messageContent)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/chat/general")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(messageContent))
                .andExpect(jsonPath("$.senderId").value(user1Id))
                .andExpect(jsonPath("$.chatType").value("GENERAL"))
                .andExpect(jsonPath("$.read").value(false))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void sendGeneralMessage_WithMedia_ReturnsMessageDto() throws Exception {
        // Arrange
        String messageContent = "Message with media";
        String mediaUrl = "https://example.com/image.jpg";
        MessageDto request = MessageDto.builder()
                .content(messageContent)
                .mediaUrl(mediaUrl)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/chat/general")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(messageContent))
                .andExpect(jsonPath("$.mediaUrl").value(mediaUrl))
                .andExpect(jsonPath("$.senderId").value(user1Id))
                .andExpect(jsonPath("$.chatType").value("GENERAL"));
    }

    @Test
    void sendGeneralMessage_EmptyContent_ReturnsBadRequest() throws Exception {
        // Arrange
        MessageDto request = MessageDto.builder()
                .content("")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/chat/general")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendGeneralMessage_NoUserId_ReturnsBadRequest() throws Exception {
        // Arrange
        MessageDto request = MessageDto.builder()
                .content("Test message")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/chat/general")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendPrivateMessage_ValidRequest_ReturnsMessageDto() throws Exception {
        // Arrange
        String messageContent = "Private message to user2";
        MessageDto request = MessageDto.builder()
                .content(messageContent)
                .recipientId(testUser2.getId())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/chat/private")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(messageContent))
                .andExpect(jsonPath("$.senderId").value(user1Id))
                .andExpect(jsonPath("$.recipientId").value(user2Id))
                .andExpect(jsonPath("$.chatType").value("PRIVATE"))
                .andExpect(jsonPath("$.read").value(false));
    }

    @Test
    void sendPrivateMessage_ToSelf_ReturnsBadRequest() throws Exception {
        // Arrange
        MessageDto request = MessageDto.builder()
                .content("Message to myself")
                .recipientId(testUser1.getId())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/chat/private")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendPrivateMessage_NonExistentRecipient_ReturnsNotFound() throws Exception {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        MessageDto request = MessageDto.builder()
                .content("Message to non-existent user")
                .recipientId(nonExistentId)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/chat/private")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getGeneralMessages_ValidRequest_ReturnsMessages() throws Exception {
        // Arrange
        // First, send some messages
        MessageDto message1 = MessageDto.builder()
                .content("First general message")
                .build();

        MessageDto message2 = MessageDto.builder()
                .content("Second general message")
                .build();

        mockMvc.perform(post("/api/chat/general")
                .header("X-User-Id", user1Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(message1)));

        mockMvc.perform(post("/api/chat/general")
                .header("X-User-Id", user2Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(message2)));

        // Act & Assert
        mockMvc.perform(get("/api/chat/general")
                        .header("X-User-Id", user1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].chatType").value("GENERAL"))
                .andExpect(jsonPath("$[1].chatType").value("GENERAL"));
    }

    @Test
    void getGeneralMessages_WithPagination_ReturnsPage() throws Exception {
        // Arrange
        // Send multiple messages
        for (int i = 1; i <= 5; i++) {
            MessageDto message = MessageDto.builder()
                    .content("Message " + i)
                    .build();
            mockMvc.perform(post("/api/chat/general")
                    .header("X-User-Id", user1Id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(message)));
        }

        // Act & Assert
        mockMvc.perform(get("/api/chat/general")
                        .header("X-User-Id", user1Id)
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void getPrivateMessages_ValidRequest_ReturnsMessages() throws Exception {
        // Arrange
        // Send private messages between users
        MessageDto message1 = MessageDto.builder()
                .content("Hello from user1")
                .recipientId(testUser2.getId())
                .build();

        MessageDto message2 = MessageDto.builder()
                .content("Hello from user2")
                .recipientId(testUser1.getId())
                .build();

        mockMvc.perform(post("/api/chat/private")
                .header("X-User-Id", user1Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(message1)));

        mockMvc.perform(post("/api/chat/private")
                .header("X-User-Id", user2Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(message2)));

        // Act & Assert - Get conversation between user1 and user2
        mockMvc.perform(get("/api/chat/room/{slug}", "private-" + user1Id + "-" + user2Id)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].chatType").value("PRIVATE"))
                .andExpect(jsonPath("$[1].chatType").value("PRIVATE"));
    }

    @Test
    void markMessageAsRead_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        // First send a private message
        MessageDto message = MessageDto.builder()
                .content("Unread message")
                .recipientId(testUser2.getId())
                .build();

        String response = mockMvc.perform(post("/api/chat/private")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(message)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        MessageDto createdMessage = objectMapper.readValue(response, MessageDto.class);
        String messageId = createdMessage.getId().toString();

        // Act & Assert - Mark as read by recipient
        mockMvc.perform(post("/api/chat/messages/{id}/read", messageId)
                        .header("X-User-Id", user2Id))
                .andExpect(status().isOk());

        // Verify message is now read
        mockMvc.perform(get("/api/chat/room/{slug}", "private-" + user1Id + "-" + user2Id)
                        .header("X-User-Id", user2Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].read").value(true));
    }

    @Test
    void markMessageAsRead_NotRecipient_ReturnsForbidden() throws Exception {
        // Arrange
        // Send a private message from user1 to user2
        MessageDto message = MessageDto.builder()
                .content("Private message")
                .recipientId(testUser2.getId())
                .build();

        String response = mockMvc.perform(post("/api/chat/private")
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(message)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        MessageDto createdMessage = objectMapper.readValue(response, MessageDto.class);
        String messageId = createdMessage.getId().toString();

        // Act & Assert - Try to mark as read by non-recipient (user1 trying to mark their own sent message)
        mockMvc.perform(post("/api/chat/messages/{id}/read", messageId)
                        .header("X-User-Id", user1Id))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUnreadCount_ValidRequest_ReturnsCount() throws Exception {
        // Arrange
        // Send multiple unread messages to user2
        MessageDto message1 = MessageDto.builder()
                .content("First unread message")
                .recipientId(testUser2.getId())
                .build();

        MessageDto message2 = MessageDto.builder()
                .content("Second unread message")
                .recipientId(testUser2.getId())
                .build();

        mockMvc.perform(post("/api/chat/private")
                .header("X-User-Id", user1Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(message1)));

        mockMvc.perform(post("/api/chat/private")
                .header("X-User-Id", user1Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(message2)));

        // Act & Assert
        mockMvc.perform(get("/api/chat/unread-count")
                        .header("X-User-Id", user2Id))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    void getUnreadCount_NoUnreadMessages_ReturnsZero() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/chat/unread-count")
                        .header("X-User-Id", user1Id))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void getGeneralMessagesAfterDate_ValidRequest_ReturnsMessages() throws Exception {
        // Arrange
        Instant afterDate = Instant.now().minusMinutes(10);

        // Send a message
        MessageDto message = MessageDto.builder()
                .content("Recent message")
                .build();

        mockMvc.perform(post("/api/chat/general")
                .header("X-User-Id", user1Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(message)));

        // Act & Assert
        mockMvc.perform(get("/api/chat/general")
                        .header("X-User-Id", user1Id)
                        .param("after", afterDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("Recent message"));
    }

    @Test
    void getGeneralMessagesAfterDate_NoMessages_ReturnsEmptyList() throws Exception {
        // Arrange
        Instant futureDate = Instant.now().plusMinutes(10);

        // Act & Assert
        mockMvc.perform(get("/api/chat/general")
                        .header("X-User-Id", user1Id)
                        .param("after", futureDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}