package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.exception.GlobalExceptionHandler;
import com.platform.model.dto.ReactionDto;
import com.platform.service.ReactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
class ReactionControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private ReactionService reactionService;
    @InjectMocks private ReactionController reactionController;

    private String postId;
    private String userId;
    private Jwt jwt;
    private JwtAuthenticationToken auth;
    private ReactionDto reactionDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reactionController)
                .addFilters(securityContextFilter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        postId = UUID.randomUUID().toString();
        userId = UUID.randomUUID().toString();

        jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("preferred_username", "testuser")
                .claim("realm_access.roles", "USER_0")
                .build();

        auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER_0")));

        reactionDto = ReactionDto.builder()
                .postId(postId)
                .userId(userId)
                .type("like")
                .userReacted(true)
                .count(5L)
                .build();
    }

    @Test
    void toggleReaction_DefaultLike_ReturnsOk() throws Exception {
        when(reactionService.toggleReaction(eq(postId), eq("like"), any(Jwt.class)))
                .thenReturn(reactionDto);

        mockMvc.perform(post("/api/posts/{postId}/reactions", postId)
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("like"))
                .andExpect(jsonPath("$.userReacted").value(true))
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    void toggleReaction_CustomType_PassesType() throws Exception {
        ReactionDto heartReaction = ReactionDto.builder()
                .postId(postId).userId(userId).type("heart").userReacted(true).count(3L).build();

        when(reactionService.toggleReaction(eq(postId), eq("heart"), any(Jwt.class)))
                .thenReturn(heartReaction);

        mockMvc.perform(post("/api/posts/{postId}/reactions", postId)
                        .param("type", "heart")
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("heart"));
    }

    @Test
    void toggleReaction_ToggleOff_ReturnsInactive() throws Exception {
        ReactionDto toggledOff = ReactionDto.builder()
                .postId(postId).userId(userId).type("like").userReacted(false).count(4L).build();

        when(reactionService.toggleReaction(eq(postId), eq("like"), any(Jwt.class)))
                .thenReturn(toggledOff);

        mockMvc.perform(post("/api/posts/{postId}/reactions", postId)
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userReacted").value(false))
                .andExpect(jsonPath("$.count").value(4));
    }

    @Test
    void getReactionStatus_UserHasLiked_ReturnsActive() throws Exception {
        when(reactionService.getReactionStatus(eq(postId), any(Jwt.class)))
                .thenReturn(reactionDto);

        mockMvc.perform(get("/api/posts/{postId}/reactions", postId)
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userReacted").value(true))
                .andExpect(jsonPath("$.type").value("like"));
    }

    @Test
    void getReactionStatus_UserHasNotReacted_ReturnsInactive() throws Exception {
        ReactionDto noReaction = ReactionDto.builder()
                .postId(postId).userId(userId).type(null).userReacted(false).count(5L).build();

        when(reactionService.getReactionStatus(eq(postId), any(Jwt.class)))
                .thenReturn(noReaction);

        mockMvc.perform(get("/api/posts/{postId}/reactions", postId)
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userReacted").value(false));
    }
}
