package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.exception.GlobalExceptionHandler;
import com.platform.model.dto.ChatRoomDto;
import com.platform.model.dto.MessageDto;
import com.platform.model.dto.QuestionDto;
import com.platform.service.ChatRoomService;
import com.platform.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.platform.controller.TestSecurityHelper.securityContextFilter;
import static com.platform.controller.TestSecurityHelper.withAuth;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private ChatService chatService;
    @Mock private ChatRoomService chatRoomService;

    @InjectMocks
    private ChatController chatController;

    private String userId;
    private Jwt userJwt;
    private Jwt adminJwt;
    private JwtAuthenticationToken userAuth;
    private JwtAuthenticationToken adminAuth;
    private MessageDto messageDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatController)
                .addFilters(securityContextFilter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        userId = UUID.randomUUID().toString();

        userJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("preferred_username", "user")
                .claim("realm_access.roles", "USER_0")
                .build();

        adminJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("preferred_username", "admin")
                .claim("realm_access.roles", "SUPER_ADMIN")
                .build();

        userAuth = new JwtAuthenticationToken(userJwt, List.of(new SimpleGrantedAuthority("ROLE_USER_0")));
        adminAuth = new JwtAuthenticationToken(adminJwt, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));

        messageDto = MessageDto.builder()
                .id(UUID.randomUUID())
                .content("Hello!")
                .userId(UUID.fromString(userId))
                .userName("user")
                .chatType("room")
                .roomSlug("general")
                .createdAt(LocalDateTime.now())
                .build();

        // Глобальные lenient stubs для методов, вызываемых с разными аргументами
        lenient().when(chatService.getMessagesByRoom(anyString(), anyInt(), any(), anyString()))
                .thenReturn(List.of(messageDto));
        lenient().when(chatService.sendMessageToRoom(anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(messageDto);
    }

    // ==================== GET /api/chat/{roomSlug} ====================

    @Test
    void getMessagesByRoom_ValidRoom_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/chat/{roomSlug}", "general")
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].content").value("Hello!"));
    }

    @Test
    void getMessagesByRoom_RoomNotFound_Returns404() throws Exception {
        when(chatService.getMessagesByRoom(eq("nonexistent"), anyInt(), any(), anyString()))
                .thenThrow(new com.platform.exception.ResourceNotFoundException("not found"));

        mockMvc.perform(get("/api/chat/{roomSlug}", "nonexistent")
                        .with(withAuth(userAuth)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMessagesByRoom_AccessDenied_Returns403() throws Exception {
        when(chatService.getMessagesByRoom(eq("private-room"), anyInt(), any(), anyString()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("no access"));

        mockMvc.perform(get("/api/chat/{roomSlug}", "private-room")
                        .with(withAuth(userAuth)))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/chat/{roomSlug} ====================

    @Test
    void sendMessageToRoom_ReturnsOk() throws Exception {
        MessageDto.SendRequest request = new MessageDto.SendRequest();
        request.setContent("Hi there");

        mockMvc.perform(post("/api/chat/{roomSlug}", "general")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hello!"));
    }

    // ==================== GET /api/chat/rooms ====================

    @Test
    void getRooms_ReturnsList() throws Exception {
        ChatRoomDto dto = ChatRoomDto.builder()
                .slug("questions-general").type("question").name("General Q&A").isActive(true).build();
        when(chatRoomService.getQuestionRoomsDto()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/chat/rooms")
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("questions-general"));
    }

    // ==================== Legacy: GET /api/chat/unread ====================

    @Test
    void getUnreadCount_ReturnsNumber() throws Exception {
        when(chatService.getUnreadCount(anyString())).thenReturn(7L);

        mockMvc.perform(get("/api/chat/unread")
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk());
    }

    // ==================== Questions DTO ====================

    @Test
    void getQuestions_ReturnsList() throws Exception {
        QuestionDto q = QuestionDto.builder()
                .id("q-1").question("How?").mode("nutrition").isAnswered(false)
                .createdAt(LocalDateTime.now()).build();
        when(chatService.getQuestionsByMode(eq("nutrition"), anyString())).thenReturn(List.of(q));

        mockMvc.perform(get("/api/chat/questions-dto/{mode}", "nutrition")
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question").value("How?"));
    }

    @Test
    void createQuestion_ReturnsCreated() throws Exception {
        QuestionDto.CreateRequest request = new QuestionDto.CreateRequest();
        request.setQuestion("What is protein?");

        QuestionDto result = QuestionDto.builder()
                .id("q-2").question("What is protein?").mode("nutrition")
                .isAnswered(false).createdAt(LocalDateTime.now()).build();

        when(chatService.createQuestion(eq("nutrition"), any(QuestionDto.CreateRequest.class), anyString()))
                .thenReturn(result);

        mockMvc.perform(post("/api/chat/questions-dto/{mode}", "nutrition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("What is protein?"));
    }

    @Test
    void answerQuestion_AdminCanAnswer() throws Exception {
        QuestionDto.AnswerRequest request = new QuestionDto.AnswerRequest();
        request.setAnswer("Protein is essential.");

        QuestionDto result = QuestionDto.builder()
                .id("q-1").question("How?").answer("Protein is essential.")
                .isAnswered(true).answeredBy("admin").mode("nutrition").build();

        when(chatService.answerQuestion(eq("q-1"), any(QuestionDto.AnswerRequest.class), anyString()))
                .thenReturn(result);

        mockMvc.perform(post("/api/chat/questions-dto/{questionId}/answer", "q-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answered").value(true));
    }

    @Test
    void deleteQuestion_Returns204() throws Exception {
        doNothing().when(chatService).deleteQuestion(eq("q-1"), anyString());

        mockMvc.perform(delete("/api/chat/questions-dto/{questionId}", "q-1")
                        .with(withAuth(adminAuth)))
                .andExpect(status().isNoContent());
    }

    @Test
    void archiveAnsweredQuestions_ReturnsCount() throws Exception {
        when(chatService.archiveAnsweredQuestions(eq("nutrition"), anyString())).thenReturn(3);

        mockMvc.perform(post("/api/chat/questions-dto/{mode}/archive", "nutrition")
                        .with(withAuth(adminAuth)))
                .andExpect(status().isOk());
    }
}
