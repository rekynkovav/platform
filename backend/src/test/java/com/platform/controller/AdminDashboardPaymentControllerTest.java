package com.platform.controller;

import com.platform.model.Payment;
import com.platform.model.dto.AdminStatsDto;
import com.platform.model.dto.PaymentDto;
import com.platform.model.dto.UserDto;
import com.platform.service.AdminService;
import com.platform.service.DashboardMenuService;
import com.platform.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminDashboardPaymentControllerTest {

    private Jwt jwt;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("550e8400-e29b-41d4-a716-446655440000")
                .build();
    }

    // ==================== AdminController ====================

    @Test
    void adminGetAllUsers_ReturnsPageFromService() {
        AdminService adminService = mock(AdminService.class);
        Page<UserDto> page = new PageImpl<>(List.of(new UserDto()));
        when(adminService.getAllUsers(isNull(), any())).thenReturn(page);

        AdminController controller = new AdminController(adminService);
        ResponseEntity<Page<UserDto>> response = controller.getAllUsers(null, 0, 50);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getTotalElements());
    }

    @Test
    void adminGetUser_ReturnsUserFromService() {
        AdminService adminService = mock(AdminService.class);
        UserDto user = new UserDto();
        when(adminService.getUserById("u-1")).thenReturn(user);

        AdminController controller = new AdminController(adminService);
        ResponseEntity<UserDto> response = controller.getUser("u-1");

        assertEquals(200, response.getStatusCode().value());
        assertSame(user, response.getBody());
    }

    @Test
    void adminBanUser_ReturnsNoContent() {
        AdminService adminService = mock(AdminService.class);

        AdminController controller = new AdminController(adminService);
        ResponseEntity<Void> response = controller.banUser("u-1", "spam", jwt);

        assertEquals(204, response.getStatusCode().value());
        verify(adminService).banUser("u-1", "spam", jwt);
    }

    @Test
    void adminUnbanUser_ReturnsNoContent() {
        AdminService adminService = mock(AdminService.class);

        AdminController controller = new AdminController(adminService);
        ResponseEntity<Void> response = controller.unbanUser("u-1", jwt);

        assertEquals(204, response.getStatusCode().value());
        verify(adminService).unbanUser("u-1", jwt);
    }

    @Test
    void adminModeratePost_ReturnsNoContent() {
        AdminService adminService = mock(AdminService.class);

        AdminController controller = new AdminController(adminService);
        ResponseEntity<Void> response = controller.moderatePost("post-1", true, null, jwt);

        assertEquals(204, response.getStatusCode().value());
        verify(adminService).moderatePost("post-1", true, null, jwt);
    }

    @Test
    void adminGetStats_ReturnsStatsFromService() {
        AdminService adminService = mock(AdminService.class);
        AdminStatsDto stats = new AdminStatsDto();
        when(adminService.getStats()).thenReturn(stats);

        AdminController controller = new AdminController(adminService);
        ResponseEntity<AdminStatsDto> response = controller.getStats();

        assertEquals(200, response.getStatusCode().value());
        assertSame(stats, response.getBody());
    }

    // ==================== DashboardController ====================

    @Test
    void dashboardGetMenu_ReturnsEmptyList() {
        DashboardMenuService menuService = mock(DashboardMenuService.class);
        when(menuService.getAllMenuData()).thenReturn(Map.of());
        DashboardController controller = new DashboardController(menuService);
        ResponseEntity<Map<String, List<Map<String, Object>>>> response = controller.getMenuData(jwt);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void dashboardSaveMenu_ReturnsSavedWithCount() {
        DashboardMenuService menuService = mock(DashboardMenuService.class);
        when(menuService.saveAllMenuData(any(), any())).thenReturn(Map.of("saved", true, "sectionsCount", 1, "itemsCount", 2));
        DashboardController controller = new DashboardController(menuService);
        Map<String, List<Map<String, Object>>> menuData = Map.of("items", List.of(Map.of("label", "Users"), Map.of("label", "Posts")));

        ResponseEntity<Map<String, Object>> response = controller.saveMenuData(menuData, jwt);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(true, response.getBody().get("saved"));
        assertEquals(2, response.getBody().get("itemsCount"));
    }

    // ==================== PaymentController ====================

    @Test
    void paymentGetMyPayments_ReturnsMappedDtos() {
        PaymentService paymentService = mock(PaymentService.class);
        Payment p = new Payment();
        p.setId(java.util.UUID.randomUUID());
        p.setAmount(new BigDecimal("990.00"));
        p.setStatus(Payment.PaymentStatus.PENDING);
        when(paymentService.getUserPayments(jwt, 50)).thenReturn(List.of(p));

        PaymentController controller = new PaymentController(paymentService);
        ResponseEntity<List<PaymentDto>> response = controller.getMyPayments(jwt, 50);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("PENDING", response.getBody().get(0).getStatus());
    }

    @Test
    void paymentGetPending_ReturnsMappedDtos() {
        PaymentService paymentService = mock(PaymentService.class);
        when(paymentService.getPendingPayments(50)).thenReturn(List.of());

        PaymentController controller = new PaymentController(paymentService);
        ResponseEntity<List<PaymentDto>> response = controller.getPendingPayments(50);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }
}
