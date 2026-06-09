package com.platform.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/webrtc")
@RequiredArgsConstructor
public class WebRTCController {

    private final Cache<String, SdpSession> offerSessions = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build();

    @Data
    public static class OfferRequest {
        @NotBlank(message = "broadcastId is required")
        private String broadcastId;
        @NotBlank(message = "sdp is required")
        private String sdp;
    }

    @Data
    public static class SdpSession {
        private String userId;
        private String sdp;
        private long createdAt;
    }

    @PostMapping("/offer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> handleOffer(
            @jakarta.validation.Valid @RequestBody OfferRequest offer,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();

        SdpSession session = new SdpSession();
        session.setUserId(userId);
        session.setSdp(offer.getSdp());
        session.setCreatedAt(System.currentTimeMillis());

        offerSessions.put(offer.getBroadcastId(), session);

        log.info("WebRTC offer received from user {} for broadcast {}", userId, offer.getBroadcastId());

        return ResponseEntity.ok(Map.of(
            "type", "answer",
            "sdp", "generated-answer-sdp" // TODO: реализовать реальный SDP answer
        ));
    }

    @GetMapping("/candidate/{broadcastId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> getIceCandidates(
            @PathVariable String broadcastId,
            @AuthenticationPrincipal Jwt jwt) {
        SdpSession session = offerSessions.getIfPresent(broadcastId);
        if (session == null || !session.getUserId().equals(jwt.getSubject())) {
            return ResponseEntity.status(403)
                .body(Map.of("error", "Access denied"));
        }
        return ResponseEntity.ok(Map.of("candidates", "[]"));
    }
}
