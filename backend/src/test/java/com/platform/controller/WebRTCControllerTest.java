package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.config.TestSecurityConfig;
import com.platform.service.WebRTCService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebRTCController.class)
@Import(TestSecurityConfig.class)
class WebRTCControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WebRTCService webRTCService;

    @Test
    void testHandleOffer_WithoutAuth_Returns401() throws Exception {
        Map<String, Object> offer = Map.of("sdp", "test-sdp", "type", "offer");

        mockMvc.perform(post("/api/webrtc/offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offer))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER_1"})
    void testHandleOffer_WithEmptyBody_Returns400() throws Exception {
        mockMvc.perform(post("/api/webrtc/offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER_1"})
    void testHandleOffer_WithValidData_Returns200() throws Exception {
        Map<String, Object> offer = Map.of(
                "sdp", "v=0\r\no=- 123456 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n",
                "type", "offer",
                "sessionId", "test-session"
        );

        Map<String, Object> answer = Map.of(
                "sdp", "v=0\r\no=- 654321 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n",
                "type", "answer"
        );

        when(webRTCService.handleOffer(any(), eq("test-user"))).thenReturn(answer);

        mockMvc.perform(post("/api/webrtc/offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offer))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sdp").exists())
                .andExpect(jsonPath("$.type").value("answer"));
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER_1"})
    void testHandleOffer_WithWrongSession_Returns403() throws Exception {
        Map<String, Object> offer = Map.of(
                "sdp", "test-sdp",
                "type", "offer",
                "sessionId", "wrong-session"
        );

        when(webRTCService.handleOffer(any(), eq("test-user")))
                .thenThrow(new SecurityException("Access denied to session"));

        mockMvc.perform(post("/api/webrtc/offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offer))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}