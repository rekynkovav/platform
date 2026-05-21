package com.platform.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        boolean ffmpegOk = checkFFmpeg();

        return ResponseEntity.ok(Map.of(
            "status", ffmpegOk ? "UP" : "DEGRADED",
            "ffmpeg", ffmpegOk ? "available" : "unavailable",
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