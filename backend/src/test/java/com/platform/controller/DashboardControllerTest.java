package com.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.exception.GlobalExceptionHandler;
import com.platform.service.DashboardMenuService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.platform.controller.TestSecurityHelper.securityContextFilter;
import static com.platform.controller.TestSecurityHelper.withAuth;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private DashboardMenuService menuService;

    @InjectMocks
    private DashboardController dashboardController;

    private Jwt adminJwt;
    private JwtAuthenticationToken adminAuth;
    private UUID adminUserId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
                .addFilters(securityContextFilter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        adminUserId = UUID.randomUUID();
        adminJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", adminUserId.toString())
                .claim("preferred_username", "admin")
                .claim("realm_access.roles", "SUPER_ADMIN")
                .build();

        adminAuth = new JwtAuthenticationToken(adminJwt, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
    }

    // ==================== GET /api/dashboard/menu ====================

    @Test
    void getMenuData_AdminCanAccess_ReturnsMap() throws Exception {
        Map<String, List<Map<String, Object>>> menuData = Map.of(
                "broadcasts", List.of(Map.of("id", "1", "title", "Test")),
                "exercises", List.of(Map.of("id", "2", "title", "Exercise"))
        );
        when(menuService.getAllMenuData()).thenReturn(menuData);

        mockMvc.perform(get("/api/dashboard/menu")
                        .with(withAuth(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.broadcasts").isArray())
                .andExpect(jsonPath("$.exercises").isArray())
                .andExpect(jsonPath("$.broadcasts[0].title").value("Test"));
    }

    // ==================== POST /api/dashboard/menu ====================

    @Test
    void saveMenuData_AdminCanSave_ReturnsOk() throws Exception {
        Map<String, List<Map<String, Object>>> menuData = Map.of(
                "broadcasts", List.of(Map.of("id", "item-1", "label", "Users", "icon", "people")),
                "exercises", List.of(Map.of("id", "item-2", "label", "Posts", "icon", "article"))
        );

        when(menuService.saveAllMenuData(any(), eq(adminUserId)))
                .thenReturn(Map.of("saved", true, "itemsCount", 2));

        mockMvc.perform(post("/api/dashboard/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menuData))
                        .with(withAuth(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true))
                .andExpect(jsonPath("$.itemsCount").value(2));
    }

    @Test
    void saveMenuData_EmptyMap_ReturnsZeroItems() throws Exception {
        Map<String, List<Map<String, Object>>> menuData = Map.of();

        when(menuService.saveAllMenuData(any(), eq(adminUserId)))
                .thenReturn(Map.of("saved", true, "itemsCount", 0));

        mockMvc.perform(post("/api/dashboard/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menuData))
                        .with(withAuth(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true))
                .andExpect(jsonPath("$.itemsCount").value(0));
    }
}
