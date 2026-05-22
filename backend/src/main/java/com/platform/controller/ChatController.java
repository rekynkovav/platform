package com.platform.controller;

import com.platform.model.dto.MessageDto;
import com.platform.model.dto.ChatRoomDto;
import com.platform.model.ChatRoom;
import com.platform.service.ChatService;
import com.platform.service.ChatRoomService;
// import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatRoomService chatRoomService;

    // ==================================================================
    // === НОВЫЕ ПОТОЛОЧНЫЕ ЭНДПОИНТЫ (рекомендуется использовать) ===
    // ==================================================================

    /**
     * Универсальный метод получения сообщений по slug комнаты — с проверкой прав
     */
    @GetMapping("/{roomSlug}")
    // @RateLimiting(name = "chat-limit")
    public ResponseEntity<List<MessageDto>> getMessagesByRoom(
            @PathVariable String roomSlug,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String before,
            @AuthenticationPrincipal Jwt jwt) {

        String userRole = jwt.getClaimAsString("realm_access.roles") != null
                ? jwt.getClaimAsString("realm_access.roles").split(",")[0]
                : "ROLE_USER_0";

        Instant beforeInstant = before != null ? Instant.parse(before) : null;
        return ResponseEntity.ok(chatService.getMessagesByRoom(roomSlug, limit, beforeInstant, userRole));
    }

    /**
     * Универсальный метод отправки сообщения в комнату — с проверкой прав
     */
    @PostMapping("/{roomSlug}")
    // @RateLimiting(name = "chat-limit")
    public ResponseEntity<MessageDto> sendMessageToRoom(
            @PathVariable String roomSlug,
            @RequestBody MessageDto.SendRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String userRole = jwt.getClaimAsString("realm_access.roles") != null
                ? jwt.getClaimAsString("realm_access.roles").split(",")[0]
                : "ROLE_USER_0";

        return ResponseEntity.ok(chatService.sendMessageToRoom(
                roomSlug,
                jwt.getSubject(),
                request.getContent(),
                request.getMediaId(),
                userRole));
    }

    // ==================================================================
    // === LEGACY ЭНДПОИНТЫ (оставлены для совместимости с текущим фронтом) ===
    // ==================================================================

    @GetMapping("/general")
    // @RateLimiting(name = "chat-limit")
    public ResponseEntity<List<MessageDto>> getGeneralMessages(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String before,
            @AuthenticationPrincipal Jwt jwt) {
        return getMessagesByRoom("user1", limit, before, jwt);
    }

    @PostMapping("/general")
    // @RateLimiting(name = "chat-limit")
    public ResponseEntity<MessageDto> sendToGeneralChat(
            @RequestBody MessageDto.SendRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return sendMessageToRoom("user1", request, jwt);
    }

    @GetMapping("/admin-group")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    // @RateLimiting(name = "chat-limit")
    public ResponseEntity<List<MessageDto>> getAdminGroupMessages(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String before,
            @AuthenticationPrincipal Jwt jwt) {
        return getMessagesByRoom("admins", limit, before, jwt); // позже создадим комнату admins
    }

    @PostMapping("/admin-group")
    // @RateLimiting(name = "chat-limit")
    public ResponseEntity<MessageDto> sendToAdminGroup(
            @RequestBody MessageDto.SendRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return sendMessageToRoom("admins", request, jwt);
    }

    // private чаты пока оставляем как есть (можно позже перевести на roomSlug = "private_" + userId)
    @GetMapping("/private/{userId}")
    // @RateLimiting(name = "chat-limit")
    public ResponseEntity<List<MessageDto>> getPrivateMessages(
            @PathVariable String userId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String before,
            @AuthenticationPrincipal Jwt jwt) {
        // временно оставляем старую реализацию
        return ResponseEntity.ok(chatService.getPrivateMessages(jwt.getSubject(), userId, limit, before));
    }

    @PostMapping("/private/{userId}")
    // @RateLimiting(name = "chat-limit")
    public ResponseEntity<MessageDto> sendPrivateMessage(
            @PathVariable String userId,
            @RequestBody MessageDto.SendRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatService.sendPrivateMessage(jwt.getSubject(),
                userId, request.getContent(), request.getMediaId()));
    }

    // остальные legacy методы (unread, markAsRead) оставляем без изменений
    @GetMapping("/unread")
    // @RateLimiting(name = "chat-limit")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatService.getUnreadCount(jwt.getSubject()));
    }

    @PutMapping("/messages/{messageId}/read")
    // @RateLimiting(name = "chat-limit")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String messageId,
            @AuthenticationPrincipal Jwt jwt) {

        chatService.markAsRead(messageId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    // ==================================================================
    // === ЭНДПОИНТЫ ДЛЯ ВКЛАДКИ "ПОМОЩЬ" (Questions) ===
    // ==================================================================

    /**
     * Возвращает список всех доступных комнат вопросов
     */
    @GetMapping("/questions/rooms")
    // @RateLimiting(name = "chat-limit")
    public ResponseEntity<List<ChatRoomDto>> getQuestionRooms() {
        return ResponseEntity.ok(chatRoomService.getQuestionRoomsDto());
    }

    /**
     * Получить сообщения из комнаты вопросов (по режиму: general / nutrition / vyacheslav)
     */
    @GetMapping("/questions/{mode}")
    // @RateLimiting(name = "chat-limit")
    public ResponseEntity<List<MessageDto>> getQuestionMessages(
            @PathVariable String mode,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String before,
            @AuthenticationPrincipal Jwt jwt) {

        ChatRoom room = chatRoomService.getQuestionRoomByMode(mode);
        Instant beforeInstant = before != null ? Instant.parse(before) : null;

        String userRole = jwt.getClaimAsString("realm_access.roles") != null
                ? jwt.getClaimAsString("realm_access.roles").split(",")[0]
                : "ROLE_USER_0";

        return ResponseEntity.ok(chatService.getMessagesByRoom(room.getSlug(), limit, beforeInstant, userRole));
    }

    /**
     * Отправить вопрос / ответ в комнату вопросов
     */
    @PostMapping("/questions/{mode}")
    // @RateLimiting(name = "chat-limit")
    public ResponseEntity<MessageDto> sendQuestionMessage(
            @PathVariable String mode,
            @RequestBody MessageDto.SendRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        ChatRoom room = chatRoomService.getQuestionRoomByMode(mode);

        String userRole = jwt.getClaimAsString("realm_access.roles") != null
                ? jwt.getClaimAsString("realm_access.roles").split(",")[0]
                : "ROLE_USER_0";

        return ResponseEntity.ok(chatService.sendMessageToRoom(
                room.getSlug(),
                jwt.getSubject(),
                request.getContent(),
                request.getMediaId(),
                userRole));
    }
}