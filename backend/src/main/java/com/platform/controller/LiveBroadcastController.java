package com.platform.controller;

import com.platform.dto.CreateBroadcastRequest;
import com.platform.model.BroadcastStatus;
import com.platform.model.LiveBroadcast;
import com.platform.model.dto.LiveBroadcastDto;
import com.platform.service.LiveBroadcastService;
import com.platform.service.MetricsService;
import com.platform.service.BroadcastArchiveService;
import com.platform.service.BroadcastChatService;
import com.platform.service.BroadcastReactionService;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.annotation.Counted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/broadcast")
@RequiredArgsConstructor
public class LiveBroadcastController {

    private final LiveBroadcastService broadcastService;
    private final MetricsService metricsService;
    private final BroadcastArchiveService broadcastArchiveService;
    private final BroadcastChatService broadcastChatService;
    private final BroadcastReactionService broadcastReactionService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('CONSULTANT_PITANIE', 'CONSULTANT_SPORT', 'MODERATOR_ADMIN', 'VYACHESLAV', 'SUPER_ADMIN')")
    @RateLimiting(name = "broadcast-limit")
    public ResponseEntity<LiveBroadcastDto> createBroadcast(
            @jakarta.validation.Valid @RequestBody CreateBroadcastRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();

        log.info("User {} creating broadcast: {}", userId, request.getTitle());

        LiveBroadcast broadcast = broadcastService.createBroadcast(
            request.getTitle(),
            request.getDescription(),
            request.getStreamType(),
            userId
        );
        return ResponseEntity.ok(broadcastService.toDto(broadcast));
    }

    /**
     * Алиас для фронтенда: создание + старт трансляции одним вызовом
     * Frontend: POST /api/broadcast/start
     */
    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('CONSULTANT_PITANIE', 'CONSULTANT_SPORT', 'MODERATOR_ADMIN', 'VYACHESLAV', 'SUPER_ADMIN')")
    @Timed(value = "broadcast.start.time", description = "Time to start broadcast")
    @Counted(value = "broadcast.start.count", description = "Number of broadcast starts")
    @RateLimiting(name = "broadcast-limit")
    public ResponseEntity<Map<String, String>> createAndStartBroadcast(
            @jakarta.validation.Valid @RequestBody CreateBroadcastRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        metricsService.incrementBroadcasts();

        LiveBroadcast broadcast = broadcastService.createBroadcast(
            request.getTitle(),
            request.getDescription(),
            request.getStreamType(),
            userId
        );

        String rtmpUrl = broadcastService.startBroadcast(broadcast.getId().toString(), userId);

        log.info("User {} created and started broadcast {}", userId, broadcast.getId());

        return ResponseEntity.ok(Map.of(
            "streamUrl", rtmpUrl,
            "streamKey", broadcast.getId().toString(),
            "status", "live"
        ));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('CONSULTANT_PITANIE', 'CONSULTANT_SPORT', 'MODERATOR_ADMIN', 'VYACHESLAV', 'SUPER_ADMIN')")
    @Timed(value = "broadcast.start.time", description = "Time to start broadcast")
    @Counted(value = "broadcast.start.count", description = "Number of broadcast starts")
    @RateLimiting(name = "broadcast-limit")
    public ResponseEntity<Map<String, String>> startBroadcast(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        metricsService.incrementBroadcasts();
        String rtmpUrl = broadcastService.startBroadcast(id, userId);

        log.info("User {} started broadcast {}", userId, id);

        return ResponseEntity.ok(Map.of(
            "streamUrl", rtmpUrl,
            "streamKey", id,
            "status", "live"
        ));
    }

    @PostMapping("/{id}/stop")
    @PreAuthorize("hasAnyRole('CONSULTANT_PITANIE', 'CONSULTANT_SPORT', 'MODERATOR_ADMIN', 'VYACHESLAV', 'SUPER_ADMIN')")
    @RateLimiting(name = "broadcast-limit")
    public ResponseEntity<Void> stopBroadcast(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        broadcastService.stopBroadcast(id, userId);

        log.info("User {} stopped broadcast {}", userId, id);

        return ResponseEntity.ok().build();
    }

    /**
     * Алиас для фронтенда: остановка трансляции через /end
     * Frontend: POST /api/broadcast/{id}/end
     */
    @PostMapping("/{id}/end")
    @PreAuthorize("hasAnyRole('CONSULTANT_PITANIE', 'CONSULTANT_SPORT', 'MODERATOR_ADMIN', 'VYACHESLAV', 'SUPER_ADMIN')")
    @RateLimiting(name = "broadcast-limit")
    public ResponseEntity<Void> endBroadcast(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        return stopBroadcast(id, jwt);
    }

    @GetMapping("/active")
    public ResponseEntity<List<LiveBroadcastDto>> getActiveBroadcasts() {
        List<LiveBroadcast> broadcasts = broadcastService.getActiveBroadcasts();
        log.debug("Active broadcasts count: {}", broadcasts.size());

        List<LiveBroadcastDto> dtos = broadcasts.stream()
            .map(broadcastService::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LiveBroadcastDto> getBroadcast(@PathVariable String id) {
        LiveBroadcast broadcast = broadcastService.getBroadcast(id);
        return ResponseEntity.ok(broadcastService.toDto(broadcast));
    }

    /**
     * Статистика трансляции для фронтенда
     * Frontend: GET /api/broadcast/{id}/stats
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getBroadcastStats(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        LiveBroadcast broadcast = broadcastService.getBroadcast(id);
        LiveBroadcastDto dto = broadcastService.toDto(broadcast);

        return ResponseEntity.ok(Map.of(
            "broadcastId", id,
            "status", dto.getStatus() != null ? dto.getStatus() : "UNKNOWN",
            "viewerCount", dto.getViewerCount() != null ? dto.getViewerCount() : 0,
            "startedAt", dto.getStartedAt() != null ? dto.getStartedAt().toString() : "",
            "endedAt", dto.getEndedAt() != null ? dto.getEndedAt().toString() : ""
        ));
    }

    @GetMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('CONSULTANT_PITANIE', 'CONSULTANT_SPORT', 'MODERATOR_ADMIN', 'VYACHESLAV', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> getArchiveUrl(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        String streamUrl = broadcastArchiveService.getArchiveStreamUrl(id, jwt.getSubject());

        return ResponseEntity.ok(Map.of(
            "streamUrl", streamUrl,
            "broadcastId", id
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    @RateLimiting(name = "broadcast-limit")
    public ResponseEntity<Void> deleteBroadcast(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        LiveBroadcast broadcast = broadcastService.getBroadcast(id);
        if (broadcast.getStatus() == BroadcastStatus.LIVE) {
            return ResponseEntity.status(409).build();
        }
        broadcastService.deleteBroadcast(id, userId);

        log.warn("User {} deleted broadcast {}", userId, id);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/playlist.m3u8")
    public ResponseEntity<String> getHlsPlaylist(@PathVariable String id) {
        String playlist = broadcastService.getHlsPlaylist(id);
        return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.apple.mpegurl")
                .body(playlist);
    }

    @PostMapping("/webhook/rtmp")
    @RateLimiting(name = "broadcast-limit")
    public ResponseEntity<Void> rtmpWebhook(
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret) {

        String expectedSecret = System.getenv("RTMP_WEBHOOK_SECRET");
        if (expectedSecret == null || expectedSecret.isBlank()) {
            log.error("RTMP_WEBHOOK_SECRET not configured - refusing webhook");
            return ResponseEntity.status(503).build();
        }
        if (secret == null || !secret.equals(expectedSecret)) {
            log.warn("Invalid or missing webhook secret");
            return ResponseEntity.status(403).build();
        }

        String action = payload.get("action");
        String streamKey = payload.get("streamKey");

        log.info("RTMP webhook: action={}, streamKey={}", action, streamKey);

        if ("publish".equals(action)) {
            broadcastService.onStreamStart(streamKey);
        } else if ("publish_done".equals(action)) {
            broadcastService.onStreamStop(streamKey);
        }

        return ResponseEntity.ok().build();
    }

    // ---- Broadcast Chat ----

    @PostMapping("/{id}/chat")
    @PreAuthorize("isAuthenticated()")
    @RateLimiting(name = "broadcast-limit")
    public ResponseEntity<Map<String, Object>> sendChatMessage(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        String content = body.get("content");
        String type = body.getOrDefault("type", "message");

        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "content is required"));
        }

        // Verify broadcast exists
        broadcastService.getBroadcast(id);

        String username = jwt.getClaimAsString("preferred_username");
        if (username == null) username = jwt.getClaimAsString("name");
        if (username == null) username = "User";

        String userRole = com.platform.util.RoleUtils.getUserRole(jwt);

        BroadcastChatService.ChatMessage message = broadcastChatService.sendMessage(
            id, userId, username, userRole, content, type
        );

        return ResponseEntity.ok(Map.of(
            "id", message.id(),
            "broadcastId", message.broadcastId(),
            "userId", message.userId(),
            "username", message.username(),
            "content", message.content(),
            "type", message.type(),
            "timestamp", message.timestamp()
        ));
    }

    @GetMapping("/{id}/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BroadcastChatService.ChatMessage>> getChatMessages(
            @PathVariable String id,
            @RequestParam(defaultValue = "50") int limit) {

        broadcastService.getBroadcast(id);
        List<BroadcastChatService.ChatMessage> messages = broadcastChatService.getMessages(id, limit);
        return ResponseEntity.ok(messages);
    }

    // ---- Broadcast Reactions ----

    @PostMapping("/{id}/reaction")
    @PreAuthorize("isAuthenticated()")
    @RateLimiting(name = "broadcast-limit")
    public ResponseEntity<Map<String, Object>> sendReaction(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {

        String emoji = body.get("emoji");
        if (emoji == null || emoji.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "emoji is required"));
        }

        broadcastService.getBroadcast(id);
        BroadcastReactionService.ReactionSummary summary = broadcastReactionService.addReaction(
            id, jwt.getSubject(), emoji
        );

        return ResponseEntity.ok(Map.of(
            "broadcastId", summary.broadcastId(),
            "counts", summary.counts(),
            "total", summary.total()
        ));
    }

    @GetMapping("/{id}/reaction")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getReactions(@PathVariable String id) {
        broadcastService.getBroadcast(id);
        BroadcastReactionService.ReactionSummary summary = broadcastReactionService.getSummary(id);
        return ResponseEntity.ok(Map.of(
            "broadcastId", summary.broadcastId(),
            "counts", summary.counts(),
            "total", summary.total()
        ));
    }

    // ---- Broadcast-Event Linking ----

    @PostMapping("/{id}/link/{eventId}")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    @RateLimiting(name = "broadcast-limit")
    public ResponseEntity<Map<String, Object>> linkToEvent(
            @PathVariable String id,
            @PathVariable String eventId,
            @AuthenticationPrincipal Jwt jwt) {

        LiveBroadcast broadcast = broadcastService.getBroadcast(id);
        broadcast.setLinkedEventId(java.util.UUID.fromString(eventId));
        broadcastService.updateBroadcast(broadcast);

        log.info("Broadcast {} linked to event {} by {}", id, eventId, jwt.getSubject());

        return ResponseEntity.ok(Map.of(
            "broadcastId", id,
            "linkedEventId", eventId,
            "status", "linked"
        ));
    }

    @DeleteMapping("/{id}/link")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    @RateLimiting(name = "broadcast-limit")
    public ResponseEntity<Map<String, Object>> unlinkFromEvent(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        LiveBroadcast broadcast = broadcastService.getBroadcast(id);
        broadcast.setLinkedEventId(null);
        broadcastService.updateBroadcast(broadcast);

        log.info("Broadcast {} unlinked from event by {}", id, jwt.getSubject());

        return ResponseEntity.ok(Map.of(
            "broadcastId", id,
            "status", "unlinked"
        ));
    }

    @GetMapping("/{id}/linked-event")
    public ResponseEntity<Map<String, Object>> getLinkedEvent(@PathVariable String id) {
        LiveBroadcast broadcast = broadcastService.getBroadcast(id);
        String eventId = broadcast.getLinkedEventId() != null ? broadcast.getLinkedEventId().toString() : null;
        return ResponseEntity.ok(Map.of(
            "broadcastId", id,
            "linkedEventId", eventId != null ? eventId : ""
        ));
    }
}
