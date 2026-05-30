package com.platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@Slf4j
public class AuditService {

    public void logAction(String userId, String action, String details) {
        log.info("AUDIT: user={}, action={}, details={}, timestamp={}",
            userId, action, details, System.currentTimeMillis());
    }

    public void logLogin(String userId, boolean success, String ip) {
        log.info("AUDIT_LOGIN: user={}, success={}, ip={}, time={}",
            userId, success, ip != null ? ip : "unknown", System.currentTimeMillis());
    }

    public void logPayment(String userId, String paymentId, String status, BigDecimal amount) {
        log.info("AUDIT_PAYMENT: user={}, paymentId={}, status={}, amount={}, currency=RUB",
            userId, paymentId, status, amount);
    }

    public void logAdminAction(String adminId, String action, String targetUserId) {
        log.info("AUDIT_ADMIN: admin={}, action={}, target={}, timestamp={}",
            adminId, action, targetUserId != null ? targetUserId : "none", System.currentTimeMillis());
    }

    public void logBroadcast(String userId, String broadcastId, String action) {
        log.info("AUDIT_BROADCAST: user={}, broadcastId={}, action={}",
            userId, broadcastId, action);
    }

    public void logError(String userId, String errorType, String message) {
        log.error("AUDIT_ERROR: user={}, errorType={}, message={}",
            userId, errorType, message);
    }
}