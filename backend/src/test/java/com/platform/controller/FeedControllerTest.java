package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.exception.GlobalExceptionHandler;
import com.platform.model.dto.PostDto;
import com.platform.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
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
class FeedControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private PostService postService;
    @InjectMocks private FeedController feedController;

    private String userId;
    private Jwt adminJwt;
    private Jwt userJwt;
    private JwtAuthenticationToken adminAuth;
    private JwtAuthenticationToken userAuth;
    private PostDto postDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(feedController)
                .addFilters(securityContextFilter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        userId = UUID.randomUUID().toString();

        adminJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("preferred_username", "admin")
                .claim("realm_access.roles", "SUPER_ADMIN")
                .build();

        userJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("preferred_username", "user")
                .claim("realm_access.roles", "USER_0")
                .build();

        adminAuth = new JwtAuthenticationToken(adminJwt, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        userAuth = new JwtAuthenticationToken(userJwt, List.of(new SimpleGrantedAuthority("ROLE_USER_0")));

        postDto = PostDto.builder()
                .id(UUID.randomUUID().toString())
                .content("Feed post content")
                .authorId(userId)
                .authorName("admin")
                .category("nutrition")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== GET /api/feed/main ====================

    @Test
    void getMainFeed_ReturnsList() throws Exception {
        when(postService.getFeed(anyString(), isNull(), any(Pageable.class)))
                .thenReturn(List.of(postDto));

        mockMvc.perform(get("/api/feed/main")
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].content").value("Feed post content"));
    }

    @Test
    void getMainFeed_WithBeforeParam_PassesCursor() throws Exception {
        String before = UUID.randomUUID().toString();
        when(postService.getFeed(anyString(), eq(before), any(Pageable.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/feed/main")
                        .param("before", before)
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getMainFeed_CustomLimit_UsesLimit() throws Exception {
        when(postService.getFeed(anyString(), isNull(), any(Pageable.class)))
                .thenReturn(List.of(postDto));

        mockMvc.perform(get("/api/feed/main")
                        .param("limit", "20")
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk());

        verify(postService).getFeed(anyString(), isNull(), argThat(p -> p.getPageSize() == 20));
    }

    // ==================== GET /api/feed/nutrition ====================

    @Test
    void getNutritionFeed_ReturnsFilteredPosts() throws Exception {
        when(postService.getFeedByCategory(eq("nutrition"), anyString(), isNull(), any(Pageable.class)))
                .thenReturn(List.of(postDto));

        mockMvc.perform(get("/api/feed/nutrition")
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("nutrition"));
    }

    @Test
    void getNutritionFeed_Empty_ReturnsEmptyArray() throws Exception {
        when(postService.getFeedByCategory(eq("nutrition"), anyString(), isNull(), any(Pageable.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/feed/nutrition")
                        .with(withAuth(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ==================== POST /api/feed/posts ====================

    @Test
    void createPost_AdminCanCreate() throws Exception {
        PostDto.CreateRequest request = new PostDto.CreateRequest();
        request.setContent("New feed content");

        when(postService.createPost(any(PostDto.CreateRequest.class), any(Jwt.class)))
                .thenReturn(postDto);

        mockMvc.perform(post("/api/feed/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Feed post content"));
    }

    // ==================== PUT /api/feed/posts/{id} ====================

    @Test
    void updatePost_AdminCanUpdate() throws Exception {
        String postId = postDto.getId();
        PostDto.UpdateRequest request = new PostDto.UpdateRequest();
        request.setContent("Updated feed content");

        PostDto updated = PostDto.builder()
                .id(postId).content("Updated feed content")
                .authorId(userId).authorName("admin").createdAt(LocalDateTime.now()).build();

        when(postService.updatePost(eq(postId), any(PostDto.UpdateRequest.class), any(Jwt.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/feed/posts/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated feed content"));
    }

    // ==================== DELETE /api/feed/posts/{id} ====================

    @Test
    void deletePost_AdminCanDelete_Returns204() throws Exception {
        String postId = postDto.getId();
        doNothing().when(postService).deletePost(eq(postId), any(Jwt.class));

        mockMvc.perform(delete("/api/feed/posts/{id}", postId)
                        .with(withAuth(adminAuth)))
                .andExpect(status().isNoContent());
    }
}
