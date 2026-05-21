package com.platform.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/webrtc")
public class WebRTCController {

    private final Map<String, String> offerSessions = new ConcurrentHashMap<>();

    @PostMapping("/offer")
    public ResponseEntity<Map<String, String>> handleOffer(@RequestBody Map<String, String> offer) {
        String broadcastId = offer.get("broadcastId");
        String sdp = offer.get("sdp");

        offerSessions.put(broadcastId, sdp);

        return ResponseEntity.ok(Map.of(
            "type", "answer",
            "sdp", "generated-answer-sdp"
        ));
    }

    @GetMapping("/candidate/{broadcastId}")
    public ResponseEntity<Map<String, String>> getIceCandidates(@PathVariable String broadcastId) {
        return ResponseEntity.ok(Map.of("candidates", "[]"));
    }
}