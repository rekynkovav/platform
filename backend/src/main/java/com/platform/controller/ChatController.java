package com.platform.controller;

import com.platform.model.dto.MessageDto;
import com.platform.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/general")
    public ResponseEntity<List<MessageDto>> getGeneralMessages(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String before) {

        return ResponseEntity.ok(chatService.getGeneralMessages(limit, before));
    }

    @PostMapping("/general")
    public ResponseEntity<MessageDto> sendToGeneralChat(
            @RequestBody MessageDto.SendRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(chatService.sendToGeneralChat(jwt.getSubject(),
                request.getContent(), request.getMediaId()));
    }

    @GetMapping("/admin-group")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    public ResponseEntity<List<MessageDto>> getAdminGroupMessages(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String before) {

        return ResponseEntity.ok(chatService.getAdminGroupMessages(limit, before));
    }

    @PostMapping("/admin-group")
    public ResponseEntity<MessageDto> sendToAdminGroup(
            @RequestBody MessageDto.SendRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(chatService.sendToAdminGroup(jwt.getSubject(),
                request.getContent(), request.getMediaId()));
    }

    @GetMapping("/private/{userId}")
    public ResponseEntity<List<MessageDto>> getPrivateMessages(
            @PathVariable String userId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String before,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(chatService.getPrivateMessages(jwt.getSubject(), userId, limit, before));
    }

    @PostMapping("/private/{userId}")
    public ResponseEntity<MessageDto> sendPrivateMessage(
            @PathVariable String userId,
            @RequestBody MessageDto.SendRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(chatService.sendPrivateMessage(jwt.getSubject(),
                userId, request.getContent(), request.getMediaId()));
    }

    @GetMapping("/unread")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatService.getUnreadCount(jwt.getSubject()));
    }

    @PutMapping("/messages/{messageId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String messageId,
            @AuthenticationPrincipal Jwt jwt) {

        chatService.markAsRead(messageId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
