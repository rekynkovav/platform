package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.exception.GlobalExceptionHandler;
import com.platform.model.dto.ContactDto;
import com.platform.service.ContactService;
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
class ContactControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private ContactService contactService;
    @InjectMocks private ContactController contactController;

    private String userId;
    private String contactId;
    private Jwt jwt;
    private JwtAuthenticationToken auth;
    private ContactDto contactDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(contactController)
                .addFilters(securityContextFilter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        userId = UUID.randomUUID().toString();
        contactId = UUID.randomUUID().toString();

        jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("preferred_username", "testuser")
                .claim("realm_access.roles", "USER_0")
                .build();

        auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER_0")));

        contactDto = ContactDto.builder()
                .id(contactId)
                .userId(userId)
                .contactUserId(UUID.randomUUID().toString())
                .contactName("My Friend")
                .status("online")
                .avatarUrl("https://example.com/avatar.png")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== GET /api/contacts ====================

    @Test
    void getContacts_NoSearch_ReturnsAll() throws Exception {
        when(contactService.getUserContacts(anyString(), isNull()))
                .thenReturn(List.of(contactDto));

        mockMvc.perform(get("/api/contacts").with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].contactName").value("My Friend"));
    }

    @Test
    void getContacts_WithSearch_PassesFilter() throws Exception {
        when(contactService.getUserContacts(anyString(), eq("friend")))
                .thenReturn(List.of(contactDto));

        mockMvc.perform(get("/api/contacts")
                        .param("search", "friend")
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(contactService).getUserContacts(anyString(), eq("friend"));
    }

    @Test
    void getContacts_Empty_ReturnsEmptyArray() throws Exception {
        when(contactService.getUserContacts(anyString(), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/contacts").with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ==================== POST /api/contacts ====================

    @Test
    void addContact_ValidRequest_ReturnsOk() throws Exception {
        String targetUserId = UUID.randomUUID().toString();
        ContactDto.AddRequest request = new ContactDto.AddRequest();
        request.setContactUserId(targetUserId);
        request.setNickname("Buddy");

        when(contactService.addContact(anyString(), eq(targetUserId), eq("Buddy")))
                .thenReturn(contactDto);

        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactName").value("My Friend"));
    }

    // ==================== DELETE /api/contacts/{contactId} ====================

    @Test
    void removeContact_Returns204() throws Exception {
        doNothing().when(contactService).removeContact(anyString(), eq(contactId));

        mockMvc.perform(delete("/api/contacts/{contactId}", contactId)
                        .with(withAuth(auth)))
                .andExpect(status().isNoContent());

        verify(contactService).removeContact(anyString(), eq(contactId));
    }

    // ==================== PUT /api/contacts/{contactId} ====================

    @Test
    void updateContactNickname_ReturnsUpdated() throws Exception {
        ContactDto.UpdateRequest request = new ContactDto.UpdateRequest();
        request.setNickname("New Name");

        ContactDto updated = ContactDto.builder()
                .id(contactId).userId(userId)
                .contactUserId(UUID.randomUUID().toString())
                .contactName("New Name")
                .createdAt(LocalDateTime.now()).build();

        when(contactService.updateContactNickname(anyString(), eq(contactId), eq("New Name")))
                .thenReturn(updated);

        mockMvc.perform(put("/api/contacts/{contactId}", contactId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactName").value("New Name"));
    }
}
