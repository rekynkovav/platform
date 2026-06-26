package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

import java.util.List;
import java.util.UUID;

import static com.platform.controller.TestSecurityHelper.securityContextFilter;
import static com.platform.controller.TestSecurityHelper.withAuth;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebRTCControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @InjectMocks private WebRTCController webRTCController;

    private String userId;
    private Jwt jwt;
    private JwtAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(webRTCController)
                .addFilters(securityContextFilter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        userId = UUID.randomUUID().toString();

        jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("preferred_username", "streamer")
                .claim("realm_access.roles", "USER_0")
                .build();

        auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER_0")));
    }

    // ==================== POST /api/webrtc/offer ====================

    @Test
    void handleOffer_ValidRequest_ReturnsOfferReceived() throws Exception {
        WebRTCController.OfferRequest request = new WebRTCController.OfferRequest();
        request.setBroadcastId("broadcast-123");
        request.setSdp("v=0\r\no=- 123456 2 IN IP4 127.0.0.1");

        mockMvc.perform(post("/api/webrtc/offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("offer_received"))
                .andExpect(jsonPath("$.broadcastId").value("broadcast-123"));
    }

    @Test
    void handleOffer_MissingBroadcastId_Returns400() throws Exception {
        WebRTCController.OfferRequest request = new WebRTCController.OfferRequest();
        request.setSdp("v=0\r\n");

        mockMvc.perform(post("/api/webrtc/offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(auth)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleOffer_MissingSdp_Returns400() throws Exception {
        WebRTCController.OfferRequest request = new WebRTCController.OfferRequest();
        request.setBroadcastId("broadcast-123");

        mockMvc.perform(post("/api/webrtc/offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withAuth(auth)))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET /api/webrtc/candidate/{broadcastId} ====================

    @Test
    void getIceCandidates_NoBroadcast_ReturnsEmpty() throws Exception {
        // New controller returns empty candidates list, not 403
        mockMvc.perform(get("/api/webrtc/candidate/{broadcastId}", "nonexistent")
                        .with(withAuth(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates").isArray());
    }
}
