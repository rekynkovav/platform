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
import com.platform.util.RoleUtils;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * WebRTC signaling controller for live broadcasts.
 *
 * Flow:
 *   1. Viewer → POST /offer (sends SDP offer) → stored, broadcaster notified via WebSocket
 *   2. Broadcaster → GET /offer/{broadcastId} (picks up viewer offers)
 *   3. Broadcaster → POST /answer/{broadcastId} (sends SDP answer back)
 *   4. Viewer → GET /answer/{broadcastId} (polls for answer — or use WebSocket push)
 *   5. Both sides → POST /candidate/{broadcastId} (exchange ICE candidates)
 */
@Slf4j
@RestController
@RequestMapping("/api/webrtc")
@RequiredArgsConstructor
public class WebRTCController {

    // broadcastId → list of pending offers (viewers waiting)
    private final Map<String, List<SdpExchange>> pendingOffers = new ConcurrentHashMap<>();

    // broadcastId → list of pending answers (viewers waiting for answer)
    private final Map<String, Map<String, String>> pendingAnswers = new ConcurrentHashMap<>();

    // broadcastId → ICE candidates from broadcaster
    private final Map<String, List<IceCandidatePayload>> broadcasterCandidates = new ConcurrentHashMap<>();

    // broadcastId → ICE candidates from viewers
    private final Map<String, Map<String, List<IceCandidatePayload>>> viewerCandidates = new ConcurrentHashMap<>();

    // TTL cache for cleanup
    private final Cache<String, Boolean> sessionTtl = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(2, TimeUnit.HOURS)
        .build();

    @Data
    public static class OfferRequest {
        @NotBlank(message = "broadcastId is required")
        private String broadcastId;
        @NotBlank(message = "sdp is required")
        private String sdp;
    }

    @Data
    public static class AnswerRequest {
        @NotBlank(message = "sdp is required")
        private String sdp;
    }

    @Data
    public static class IceCandidateRequest {
        @NotBlank(message = "candidate is required")
        private String candidate;
        private String sdpMid;
        private Integer sdpMLineIndex;
    }

    @Data
    public static class SdpExchange {
        private String userId;
        private String sdp;
        private long createdAt;

        public SdpExchange(String userId, String sdp) {
            this.userId = userId;
            this.sdp = sdp;
            this.createdAt = System.currentTimeMillis();
        }
    }

    @Data
    public static class IceCandidatePayload {
        private String candidate;
        private String sdpMid;
        private int sdpMLineIndex;

        public IceCandidatePayload(String candidate, String sdpMid, int sdpMLineIndex) {
            this.candidate = candidate;
            this.sdpMid = sdpMid;
            this.sdpMLineIndex = sdpMLineIndex;
        }
    }

    /**
     * Viewer sends SDP offer.
     * Stored for broadcaster to pick up.
     */
    @PostMapping("/offer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> handleOffer(
            @jakarta.validation.Valid @RequestBody OfferRequest offer,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        String broadcastId = offer.getBroadcastId();

        SdpExchange exchange = new SdpExchange(userId, offer.getSdp());
        pendingOffers.computeIfAbsent(broadcastId, k -> new ArrayList<>()).add(exchange);
        sessionTtl.put(broadcastId, true);

        log.info("WebRTC offer stored: user={} broadcast={}", userId, broadcastId);

        return ResponseEntity.ok(Map.of(
            "status", "offer_received",
            "broadcastId", broadcastId,
            "message", "Offer stored. Broadcaster will pick it up and generate an answer."
        ));
    }

    /**
     * Broadcaster retrieves pending offers for a broadcast.
     */
    @GetMapping("/offer/{broadcastId}")
    @PreAuthorize("hasAnyRole('CONSULTANT_PITANIE', 'CONSULTANT_SPORT', 'MODERATOR_ADMIN', 'VYACHESLAV', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getPendingOffers(
            @PathVariable String broadcastId,
            @AuthenticationPrincipal Jwt jwt) {

        List<SdpExchange> offers = pendingOffers.getOrDefault(broadcastId, Collections.emptyList());

        // Return and clear
        pendingOffers.put(broadcastId, new ArrayList<>());

        List<Map<String, String>> offerList = offers.stream()
            .map(o -> Map.of("userId", o.getUserId(), "sdp", o.getSdp()))
            .toList();

        log.info("Broadcaster {} retrieved {} offers for broadcast {}",
            jwt.getSubject(), offerList.size(), broadcastId);

        return ResponseEntity.ok(Map.of(
            "offers", offerList,
            "count", offerList.size()
        ));
    }

    /**
     * Broadcaster sends SDP answer for a specific viewer.
     */
    @PostMapping("/answer/{broadcastId}/{viewerId}")
    @PreAuthorize("hasAnyRole('CONSULTANT_PITANIE', 'CONSULTANT_SPORT', 'MODERATOR_ADMIN', 'VYACHESLAV', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> handleAnswer(
            @PathVariable String broadcastId,
            @PathVariable String viewerId,
            @jakarta.validation.Valid @RequestBody AnswerRequest answer,
            @AuthenticationPrincipal Jwt jwt) {

        pendingAnswers
            .computeIfAbsent(broadcastId, k -> new ConcurrentHashMap<>())
            .put(viewerId, answer.getSdp());

        sessionTtl.put(broadcastId, true);

        log.info("WebRTC answer stored: broadcast={} viewer={}", broadcastId, viewerId);

        return ResponseEntity.ok(Map.of(
            "status", "answer_received",
            "viewerId", viewerId
        ));
    }

    /**
     * Viewer polls for SDP answer.
     */
    @GetMapping("/answer/{broadcastId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAnswer(
            @PathVariable String broadcastId,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        Map<String, String> answers = pendingAnswers.getOrDefault(broadcastId, Collections.emptyMap());

        String answer = answers.remove(userId);

        if (answer == null) {
            return ResponseEntity.ok(Map.of(
                "status", "waiting",
                "message", "No answer yet. Try again in a moment."
            ));
        }

        log.info("WebRTC answer delivered: broadcast={} viewer={}", broadcastId, userId);

        return ResponseEntity.ok(Map.of(
            "status", "answer_ready",
            "sdp", answer
        ));
    }

    /**
     * Exchange ICE candidates.
     * Broadcaster posts → viewers retrieve.
     * Viewer posts → broadcaster retrieves.
     */
    @PostMapping("/candidate/{broadcastId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> addIceCandidate(
            @PathVariable String broadcastId,
            @jakarta.validation.Valid @RequestBody IceCandidateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        IceCandidatePayload payload = new IceCandidatePayload(
            request.getCandidate(),
            request.getSdpMid(),
            request.getSdpMLineIndex() != null ? request.getSdpMLineIndex() : 0
        );

        // Determine if broadcaster or viewer based on role
        String userRole = RoleUtils.getUserRole(jwt);
        boolean isBroadcaster = RoleUtils.isAdmin(userRole);

        if (isBroadcaster) {
            broadcasterCandidates
                .computeIfAbsent(broadcastId, k -> new ArrayList<>())
                .add(payload);
        } else {
            viewerCandidates
                .computeIfAbsent(broadcastId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(userId, k -> new ArrayList<>())
                .add(payload);
        }

        sessionTtl.put(broadcastId, true);

        return ResponseEntity.ok(Map.of("status", "candidate_received"));
    }

    /**
     * Broadcaster retrieves viewer ICE candidates.
     */
    @GetMapping("/candidate/{broadcastId}/viewers")
    @PreAuthorize("hasAnyRole('CONSULTANT_PITANIE', 'CONSULTANT_SPORT', 'MODERATOR_ADMIN', 'VYACHESLAV', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getViewerCandidates(
            @PathVariable String broadcastId) {

        Map<String, List<IceCandidatePayload>> candidates =
            viewerCandidates.getOrDefault(broadcastId, Collections.emptyMap());

        // Convert to serializable format
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        candidates.forEach((viewerId, list) -> {
            result.put(viewerId, list.stream()
                .map(c -> Map.<String, Object>of(
                    "candidate", c.getCandidate(),
                    "sdpMid", c.getSdpMid() != null ? c.getSdpMid() : "",
                    "sdpMLineIndex", c.getSdpMLineIndex()
                ))
                .toList());
        });

        // Clear after retrieval
        viewerCandidates.put(broadcastId, new ConcurrentHashMap<>());

        return ResponseEntity.ok(Map.of("candidates", result));
    }

    /**
     * Viewer retrieves broadcaster ICE candidates.
     */
    @GetMapping("/candidate/{broadcastId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getBroadcasterCandidates(
            @PathVariable String broadcastId) {

        List<IceCandidatePayload> candidates =
            broadcasterCandidates.getOrDefault(broadcastId, Collections.emptyList());

        List<Map<String, Object>> result = candidates.stream()
            .map(c -> Map.<String, Object>of(
                "candidate", c.getCandidate(),
                "sdpMid", c.getSdpMid() != null ? c.getSdpMid() : "",
                "sdpMLineIndex", c.getSdpMLineIndex()
            ))
            .toList();

        // Clear after retrieval
        broadcasterCandidates.put(broadcastId, new ArrayList<>());

        return ResponseEntity.ok(Map.of("candidates", result));
    }
}
