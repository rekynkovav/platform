package com.platform.controller;

import com.platform.dto.CreateBroadcastRequest;
import com.platform.model.LiveBroadcast;
import com.platform.model.dto.LiveBroadcastDto;
import com.platform.service.LiveBroadcastService;
import com.platform.service.MetricsService;
import com.platform.service.BroadcastArchiveService;
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

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('CONSULTANT_PITANIE', 'CONSULTANT_SPORT', 'MODERATOR_ADMIN', 'DEVELOPER', 'VYACHESLAV', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('CONSULTANT_PITANIE', 'CONSULTANT_SPORT', 'MODERATOR_ADMIN', 'DEVELOPER', 'VYACHESLAV', 'SUPER_ADMIN')")
    @Timed(value = "broadcast.start.time", description = "Time to start broadcast")
    @Counted(value = "broadcast.start.count", description = "Number of broadcast starts")
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
    @PreAuthorize("hasAnyRole('CONSULTANT_PITANIE', 'CONSULTANT_SPORT', 'MODERATOR_ADMIN', 'DEVELOPER', 'VYACHESLAV', 'SUPER_ADMIN')")
    @Timed(value = "broadcast.start.time", description = "Time to start broadcast")
    @Counted(value = "broadcast.start.count", description = "Number of broadcast starts")
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
    @PreAuthorize("hasAnyRole('CONSULTANT_PITANIE', 'CONSULTANT_SPORT', 'MODERATOR_ADMIN', 'DEVELOPER', 'VYACHESLAV', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('CONSULTANT_PITANIE', 'CONSULTANT_SPORT', 'MODERATOR_ADMIN', 'DEVELOPER', 'VYACHESLAV', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('CONSULTANT_PITANIE', 'CONSULTANT_SPORT', 'MODERATOR_ADMIN', 'DEVELOPER', 'VYACHESLAV', 'SUPER_ADMIN')")
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
    public ResponseEntity<Void> deleteBroadcast(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
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
    public ResponseEntity<Void> rtmpWebhook(
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret) {

        String expectedSecret = System.getenv("RTMP_WEBHOOK_SECRET");
        if (expectedSecret == null || expectedSecret.isBlank()) {
            log.warn("RTMP_WEBHOOK_SECRET not configured - webhook security disabled");
        } else {
            if (secret == null || !secret.equals(expectedSecret)) {
                log.warn("Invalid or missing webhook secret");
                return ResponseEntity.status(403).build();
            }
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
}
