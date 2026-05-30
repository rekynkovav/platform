package com.platform.util;

import org.springframework.stereotype.Component;
import java.util.UUID;
import java.time.Instant;

@Component
public class IdGenerator {

    public String generateUserId() {
        return "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public String generateMessageId() {
        return "msg_" + Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public String generatePaymentId() {
        return "pay_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6);
    }

    public String generateBroadcastId() {
        return "brd_" + UUID.randomUUID().toString();
    }

    public String generateEventId() {
        return "evt_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 4);
    }

    public String generateContactId() {
        return "cnt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public String generateNotificationId() {
        return "not_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6);
    }
}