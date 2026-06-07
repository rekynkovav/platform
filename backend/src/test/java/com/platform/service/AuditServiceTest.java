package com.platform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AuditServiceTest {

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService();
    }

    // ==================== logAction ====================

    @Test
    void logAction_DoesNotThrow() {
        assertDoesNotThrow(() ->
                auditService.logAction("user-1", "PROFILE_UPDATE", "Changed email"));
    }

    @Test
    void logAction_NullDetails_DoesNotThrow() {
        assertDoesNotThrow(() ->
                auditService.logAction("user-1", "VIEW", null));
    }

    // ==================== logLogin ====================

    @Test
    void logLogin_Success_DoesNotThrow() {
        assertDoesNotThrow(() ->
                auditService.logLogin("user-1", true, "192.168.1.1"));
    }

    @Test
    void logLogin_Failure_DoesNotThrow() {
        assertDoesNotThrow(() ->
                auditService.logLogin("user-1", false, "10.0.0.1"));
    }

    @Test
    void logLogin_NullIp_DoesNotThrow() {
        assertDoesNotThrow(() ->
                auditService.logLogin("user-1", true, null));
    }

    // ==================== logPayment ====================

    @Test
    void logPayment_DoesNotThrow() {
        assertDoesNotThrow(() ->
                auditService.logPayment("user-1", "pay-123", "CONFIRMED", new BigDecimal("999.00")));
    }

    @Test
    void logPayment_ZeroAmount_DoesNotThrow() {
        assertDoesNotThrow(() ->
                auditService.logPayment("user-1", "pay-456", "PENDING", BigDecimal.ZERO));
    }

    // ==================== logAdminAction ====================

    @Test
    void logAdminAction_WithTarget_DoesNotThrow() {
        assertDoesNotThrow(() ->
                auditService.logAdminAction("admin-1", "BAN_USER", "target-user-1"));
    }

    @Test
    void logAdminAction_NullTarget_DoesNotThrow() {
        assertDoesNotThrow(() ->
                auditService.logAdminAction("admin-1", "SYSTEM_CONFIG", null));
    }

    // ==================== logBroadcast ====================

    @Test
    void logBroadcast_DoesNotThrow() {
        assertDoesNotThrow(() ->
                auditService.logBroadcast("user-1", "bc-123", "STARTED"));
    }

    // ==================== logError ====================

    @Test
    void logError_DoesNotThrow() {
        assertDoesNotThrow(() ->
                auditService.logError("user-1", "PAYMENT_FAILED", "Timeout connecting to bank"));
    }

    @Test
    void logError_NullMessage_DoesNotThrow() {
        assertDoesNotThrow(() ->
                auditService.logError("user-1", "UNKNOWN", null));
    }
}
