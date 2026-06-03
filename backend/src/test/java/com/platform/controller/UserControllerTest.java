package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.exception.GlobalExceptionHandler;
import com.platform.model.User;
import com.platform.model.dto.UserDto;
import com.platform.service.UserService;
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
import java.util.Optional;
import java.util.UUID;

import static com.platform.controller.TestSecurityHelper.securityContextFilter;
import static com.platform.controller.TestSecurityHelper.withAuth;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private UserService userService;
    @InjectMocks private UserController userController;

    private String userId;
    private Jwt jwtToken;
    private JwtAuthenticationToken auth;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .addFilters(securityContextFilter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        userId = UUID.randomUUID().toString();

        jwtToken = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("preferred_username", "testuser")
                .claim("realm_access.roles", "USER_0")
                .build();

        auth = new JwtAuthenticationToken(jwtToken, List.of(new SimpleGrantedAuthority("ROLE_USER_0")));

        user = User.builder()
                .id(UUID.fromString(userId))
                .username("testuser")
                .email("test@example.com")
                .phone("+79991234567")
                .role("USER_0")
                .avatarUrl("https://example.com/avatar.png")
                .bio("Hello world")
                .banned(false)
                .banReason(null)
                .lastActiveAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now().minusDays(30))
                .build();
    }

    @Test
    void getCurrentUser_Exists_ReturnsFullProfile() throws Exception {
        when(userService.getUserById(userId)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/me")
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.phone").value("+79991234567"))
                .andExpect(jsonPath("$.bio").value("Hello world"))
                .andExpect(jsonPath("$.banned").value(false));
    }

    @Test
    void getCurrentUser_NotFound_Returns404() throws Exception {
        when(userService.getUserById(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me")
                        .with(withAuth(auth)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCurrentUser_ValidRequest_ReturnsUpdated() throws Exception {
        UserDto.UpdateRequest request = new UserDto.UpdateRequest();
        request.setBio("Updated bio");
        request.setPhone("+79999999999");

        User updated = User.builder()
                .id(UUID.fromString(userId))
                .username("testuser")
                .email("test@example.com")
                .phone("+79999999999")
                .role("USER_0")
                .avatarUrl("https://example.com/avatar.png")
                .bio("Updated bio")
                .lastActiveAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now().minusDays(30))
                .build();

        when(userService.updateUserProfile(eq(userId), any(UserDto.UpdateRequest.class)))
                .thenReturn(Optional.of(updated));

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Updated bio"))
                .andExpect(jsonPath("$.phone").value("+79999999999"));
    }

    @Test
    void updateCurrentUser_UserNotFound_Returns404() throws Exception {
        UserDto.UpdateRequest request = new UserDto.UpdateRequest();
        request.setBio("New bio");

        when(userService.updateUserProfile(eq(userId), any(UserDto.UpdateRequest.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(auth)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserById_Exists_ReturnsPublicProfile() throws Exception {
        when(userService.getUserById(userId)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/{userId}", userId)
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.bio").value("Hello world"));
    }

    @Test
    void getUserById_NotFound_Returns404() throws Exception {
        String unknownId = UUID.randomUUID().toString();
        when(userService.getUserById(unknownId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/{userId}", unknownId)
                        .with(withAuth(auth)))
                .andExpect(status().isNotFound());
    }
}
