package com.platform.controller;

import com.platform.model.LiveBroadcast;
import com.platform.service.LiveBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/broadcast")
@RequiredArgsConstructor
public class LiveBroadcastController {

    private final LiveBroadcastService broadcastService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('CREATOR', 'MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV', 'DEVELOPER')")
    public ResponseEntity<LiveBroadcast> createBroadcast(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal Jwt jwt) {
        String title = request.get("title");
        String description = request.get("description");
        String streamType = request.getOrDefault("streamType", "video");
        String userId = jwt.getSubject();
        
        LiveBroadcast broadcast = broadcastService.createBroadcast(title, description, streamType, userId);
        return ResponseEntity.ok(broadcast);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('CREATOR', 'MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV', 'DEVELOPER')")
    public ResponseEntity<Map<String, String>> startBroadcast(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String rtmpUrl = broadcastService.startBroadcast(id, userId);
        return ResponseEntity.ok(Map.of(
            "streamUrl", rtmpUrl,
            "streamKey", id,
            "status", "live"
        ));
    }

    @PostMapping("/{id}/stop")
    @PreAuthorize("hasAnyRole('CREATOR', 'MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV', 'DEVELOPER')")
    public ResponseEntity<Void> stopBroadcast(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        broadcastService.stopBroadcast(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/active")
    public ResponseEntity<List<LiveBroadcast>> getActiveBroadcasts() {
        return ResponseEntity.ok(broadcastService.getActiveBroadcasts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LiveBroadcast> getBroadcast(@PathVariable String id) {
        return ResponseEntity.ok(broadcastService.getBroadcast(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    public ResponseEntity<Void> deleteBroadcast(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        broadcastService.deleteBroadcast(id, userId);
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
            @RequestBody Map<String, String> payload) {
        String action = payload.get("action");
        String streamKey = payload.get("streamKey");
        
        if ("publish".equals(action)) {
            broadcastService.onStreamStart(streamKey);
        } else if ("publish_done".equals(action)) {
            broadcastService.onStreamStop(streamKey);
        }
        
        return ResponseEntity.ok().build();
    }
}
