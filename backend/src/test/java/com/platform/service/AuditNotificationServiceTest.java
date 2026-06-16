package com.platform.service;

import com.platform.model.ChatRoom;
import com.platform.model.Message;
import com.platform.model.Notification;
import com.platform.repository.NotificationRepository;
import com.platform.websocket.ChatWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditNotificationServiceTest {

    // ==================== AuditService ====================

    @Test
    void auditLogAction_DoesNotThrow() {
        AuditService service = new AuditService();
        assertDoesNotThrow(() -> service.logAction("user-1", "CREATE_POST", "post-123"));
    }

    @Test
    void auditLogLogin_WithIp_DoesNotThrow() {
        AuditService service = new AuditService();
        assertDoesNotThrow(() -> service.logLogin("user-1", true, "192.168.1.1"));
    }

    @Test
    void auditLogLogin_NullIp_DoesNotThrow() {
        AuditService service = new AuditService();
        assertDoesNotThrow(() -> service.logLogin("user-1", false, null));
    }

    @Test
    void auditLogPayment_DoesNotThrow() {
        AuditService service = new AuditService();
        assertDoesNotThrow(() -> service.logPayment("user-1", "pay-1", "COMPLETED", new BigDecimal("990.00")));
    }

    @Test
    void auditLogAdminAction_WithTarget_DoesNotThrow() {
        AuditService service = new AuditService();
        assertDoesNotThrow(() -> service.logAdminAction("admin-1", "BAN_USER", "user-2"));
    }

    @Test
    void auditLogAdminAction_NullTarget_DoesNotThrow() {
        AuditService service = new AuditService();
        assertDoesNotThrow(() -> service.logAdminAction("admin-1", "VIEW_STATS", null));
    }

    @Test
    void auditLogBroadcast_DoesNotThrow() {
        AuditService service = new AuditService();
        assertDoesNotThrow(() -> service.logBroadcast("user-1", "bc-1", "START"));
    }

    @Test
    void auditLogError_DoesNotThrow() {
        AuditService service = new AuditService();
        assertDoesNotThrow(() -> service.logError("user-1", "UPLOAD_FAILED", "File too large"));
    }

    // ==================== NotificationService ====================

    private NotificationRepository notificationRepository;
    private ChatWebSocketHandler webSocketHandler;
    private org.springframework.cache.CacheManager cacheManager;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        webSocketHandler = mock(ChatWebSocketHandler.class);
        cacheManager = mock(org.springframework.cache.CacheManager.class);
        notificationService = new NotificationService(notificationRepository, webSocketHandler, cacheManager);
    }

    @Test
    void notificationCreateAndSend_SavesAndSendsViaWebSocket() {
        Notification saved = Notification.builder()
                .id(1L).userId("u1").type("info").title("Test").message("msg")
                .read(false).build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        Notification result = notificationService.createAndSend("u1", "info", "Test", "msg", "123", "room-1");

        assertEquals("u1", result.getUserId());
        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketHandler).sendNotificationToUser(eq("u1"), any(Notification.class));
    }

    @Test
    void notificationCreateAndSend_NullRelatedId_HandledGracefully() {
        Notification saved = Notification.builder().id(2L).userId("u1").type("info")
                .title("T").message("m").read(false).build();
        when(notificationRepository.save(any())).thenReturn(saved);

        assertDoesNotThrow(() ->
                notificationService.createAndSend("u1", "info", "T", "m", null, null));
    }

    @Test
    void notificationGetUnread_DelegatesToRepository() {
        when(notificationRepository.findByUserIdAndReadFalse("u1")).thenReturn(List.of());

        List<Notification> result = notificationService.getUnreadForUser("u1");

        assertNotNull(result);
        verify(notificationRepository).findByUserIdAndReadFalse("u1");
    }

    @Test
    void notificationMarkAsRead_UpdatesAndSaves() {
        Notification n = Notification.builder().id(5L).userId("u1").read(false).build();
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));

        org.springframework.cache.Cache cache = mock(org.springframework.cache.Cache.class);
        when(cacheManager.getCache("unreadNotifications")).thenReturn(cache);
        when(cacheManager.getCache("allNotifications")).thenReturn(cache);

        notificationService.markAsRead("5");

        assertTrue(n.getRead());
        verify(notificationRepository).save(n);
        verify(cache, times(2)).evict("u1");
    }

    @Test
    void notificationMarkAsRead_NotFound_ThrowsRuntimeException() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> notificationService.markAsRead("999"));
    }

    @Test
    void notificationMarkAsReadForUser_WrongOwner_ThrowsSecurityException() {
        Notification n = Notification.builder().id(10L).userId("other-user").read(false).build();
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(n));

        assertThrows(SecurityException.class, () ->
                notificationService.markAsReadForUser(10L, "u1"));
    }

    @Test
    void notificationMarkAllAsRead_MarksAllUnread() {
        org.springframework.cache.Cache cache = mock(org.springframework.cache.Cache.class);
        when(cacheManager.getCache("unreadNotifications")).thenReturn(cache);
        when(cacheManager.getCache("allNotifications")).thenReturn(cache);

        notificationService.markAllAsRead("u1");

        verify(notificationRepository).markAllAsReadByUserId("u1");
        verify(cache, times(2)).evict("u1");
    }
}
