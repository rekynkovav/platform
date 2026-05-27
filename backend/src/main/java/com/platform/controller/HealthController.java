package com.platform.controller;

import com.platform.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final ChatWebSocketHandler chatWebSocketHandler;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        boolean ffmpegOk = checkFFmpeg();

        return ResponseEntity.ok(Map.of(
            "status", ffmpegOk ? "UP" : "DEGRADED",
            "ffmpeg", ffmpegOk ? "available" : "unavailable",
            "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/websocket")
    public ResponseEntity<Map<String, Object>> websocketHealth() {
        boolean healthy = chatWebSocketHandler.isHealthy();
        return ResponseEntity.ok(Map.of(
            "status", healthy ? "UP" : "DEGRADED",
            "activeSessions", chatWebSocketHandler.getActiveSessionCount(),
            "timestamp", System.currentTimeMillis()
        ));
    }

    private boolean checkFFmpeg() {
        try {
            Process process = Runtime.getRuntime().exec("ffmpeg -version");
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}