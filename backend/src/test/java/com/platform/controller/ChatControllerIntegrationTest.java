package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.model.dto.MessageDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private com.platform.service.ChatService chatService;

    @Test
    @WithMockUser
    void getMessagesByRoom_WithValidJwt_ReturnsOk() throws Exception {
        // Arrange
        String roomSlug = "user1";
        String userId = UUID.randomUUID().toString();

        MessageDto messageDto = MessageDto.builder()
                .id(UUID.randomUUID())
                .content("Test message")
                .userId(UUID.fromString(userId))
                .userName("Test User")
                .roomSlug(roomSlug)
                .build();

        when(chatService.getMessagesByRoom(anyString(), any(), any(), anyString()))
                .thenReturn(java.util.List.of(messageDto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/chat/{roomSlug}", roomSlug)
                        .with(jwt().jwt(jwt -> {
                            jwt.claim("sub", userId);
                            jwt.claim("realm_access.roles", "ROLE_USER_0");
                        })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Test message"))
                .andExpect(jsonPath("$[0].roomSlug").value(roomSlug));
    }

    @Test
    @WithMockUser
    void sendMessageToRoom_WithValidRequest_ReturnsOk() throws Exception {
        // Arrange
        String roomSlug = "user1";
        String userId = UUID.randomUUID().toString();

        MessageDto.SendRequest sendRequest = new MessageDto.SendRequest();
        sendRequest.setContent("Test message content");
        sendRequest.setMediaId(null);

        MessageDto responseDto = MessageDto.builder()
                .id(UUID.randomUUID())
                .content("Test message content")
                .userId(UUID.fromString(userId))
                .userName("Test User")
                .roomSlug(roomSlug)
                .build();

        when(chatService.sendMessageToRoom(anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat/{roomSlug}", roomSlug)
                        .with(jwt().jwt(jwt -> {
                            jwt.claim("sub", userId);
                            jwt.claim("realm_access.roles", "ROLE_USER_0");
                        }))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Test message content"))
                .andExpect(jsonPath("$.roomSlug").value(roomSlug));
    }

    @Test
    @WithMockUser
    void getQuestionMessages_WithValidMode_ReturnsOk() throws Exception {
        // Arrange
        String mode = "general";
        String userId = UUID.randomUUID().toString();

        MessageDto messageDto = MessageDto.builder()
                .id(UUID.randomUUID())
                .content("Question message")
                .userId(UUID.fromString(userId))
                .userName("Test User")
                .roomSlug("questions-general")
                .build();

        when(chatService.getMessagesByRoom(anyString(), any(), any(), anyString()))
                .thenReturn(java.util.List.of(messageDto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/chat/questions/{mode}", mode)
                        .with(jwt().jwt(jwt -> {
                            jwt.claim("sub", userId);
                            jwt.claim("realm_access.roles", "ROLE_USER_0");
                        })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Question message"));
    }

    @Test
    @WithMockUser
    void sendQuestionMessage_WithValidRequest_ReturnsOk() throws Exception {
        // Arrange
        String mode = "general";
        String userId = UUID.randomUUID().toString();

        MessageDto.SendRequest sendRequest = new MessageDto.SendRequest();
        sendRequest.setContent("Question content");
        sendRequest.setMediaId(null);

        MessageDto responseDto = MessageDto.builder()
                .id(UUID.randomUUID())
                .content("Question content")
                .userId(UUID.fromString(userId))
                .userName("Test User")
                .roomSlug("questions-general")
                .build();

        when(chatService.sendMessageToRoom(anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat/questions/{mode}", mode)
                        .with(jwt().jwt(jwt -> {
                            jwt.claim("sub", userId);
                            jwt.claim("realm_access.roles", "ROLE_USER_0");
                        }))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Question content"));
    }

    @Test
    @WithMockUser
    void getMessagesByRoom_WithoutJwt_ReturnsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/chat/user1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void sendMessageToRoom_WithEmptyContent_ReturnsBadRequest() throws Exception {
        // Arrange
        String roomSlug = "user1";
        String userId = UUID.randomUUID().toString();

        MessageDto.SendRequest sendRequest = new MessageDto.SendRequest();
        sendRequest.setContent("");
        sendRequest.setMediaId(null);

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat/{roomSlug}", roomSlug)
                        .with(jwt().jwt(jwt -> {
                            jwt.claim("sub", userId);
                            jwt.claim("realm_access.roles", "ROLE_USER_0");
                        }))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getMessagesByRoom_WithInvalidRoom_ReturnsNotFound() throws Exception {
        // Arrange
        String invalidRoomSlug = "non-existent-room";
        String userId = UUID.randomUUID().toString();

        when(chatService.getMessagesByRoom(anyString(), any(), any(), anyString()))
                .thenThrow(new com.platform.exception.ResourceNotFoundException("Chat room not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/chat/{roomSlug}", invalidRoomSlug)
                        .with(jwt().jwt(jwt -> {
                            jwt.claim("sub", userId);
                            jwt.claim("realm_access.roles", "ROLE_USER_0");
                        })))
                .andExpect(status().isNotFound());
    }
}