package com.platform.controller;

import com.platform.model.dto.ReactionDto;
import com.platform.service.ReactionService;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/reactions")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    @PostMapping
    @RateLimiting(name = "reaction-limit")
    public ResponseEntity<ReactionDto> toggleReaction(
            @PathVariable String postId,
            @RequestParam(defaultValue = "like") String type,
            @AuthenticationPrincipal Jwt jwt) {

        ReactionDto reaction = reactionService.toggleReaction(postId, type, jwt);
        return ResponseEntity.ok(reaction);
    }

    @GetMapping
    public ResponseEntity<ReactionDto> getReactionStatus(
            @PathVariable String postId,
            @AuthenticationPrincipal Jwt jwt) {

        ReactionDto reaction = reactionService.getReactionStatus(postId, jwt);
        return ResponseEntity.ok(reaction);
    }
}