package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.exception.GlobalExceptionHandler;
import com.platform.model.dto.CommentDto;
import com.platform.service.CommentService;
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
class CommentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private CommentService commentService;
    @InjectMocks private CommentController commentController;

    private String userId;
    private String postId;
    private Jwt jwt;
    private JwtAuthenticationToken auth;
    private CommentDto commentDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(commentController)
                .addFilters(securityContextFilter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        userId = UUID.randomUUID().toString();
        postId = UUID.randomUUID().toString();

        jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("preferred_username", "testuser")
                .claim("realm_access.roles", "USER_0")
                .build();

        auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER_0")));

        commentDto = CommentDto.builder()
                .id(UUID.randomUUID().toString())
                .content("Test comment")
                .postId(postId)
                .authorId(userId)
                .authorName("testuser")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== GET /api/posts/{postId}/comments ====================

    @Test
    void getComments_ReturnsList() throws Exception {
        when(commentService.getCommentsByPost(eq(postId), anyInt())).thenReturn(List.of(commentDto));

        mockMvc.perform(get("/api/posts/{postId}/comments", postId)
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].content").value("Test comment"))
                .andExpect(jsonPath("$[0].authorName").value("testuser"));
    }

    @Test
    void getComments_Empty_ReturnsEmptyArray() throws Exception {
        when(commentService.getCommentsByPost(eq(postId), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/posts/{postId}/comments", postId)
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ==================== POST /api/posts/{postId}/comments ====================

    @Test
    void createComment_ValidRequest_ReturnsCreated() throws Exception {
        CommentDto.CreateRequest request = new CommentDto.CreateRequest();
        request.setContent("New comment");

        when(commentService.createComment(eq(postId), any(CommentDto.CreateRequest.class), any(Jwt.class)))
                .thenReturn(commentDto);

        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Test comment"))
                .andExpect(jsonPath("$.authorId").value(userId));
    }

    // ==================== DELETE /api/comments/{commentId} ====================

    @Test
    void deleteComment_OwnerCanDelete_Returns204() throws Exception {
        String commentId = commentDto.getId();
        doNothing().when(commentService).deleteComment(eq(postId), eq(commentId), any(Jwt.class));

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", postId, commentId)
                        .with(withAuth(auth)))
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(postId, commentId, jwt);
    }
}
