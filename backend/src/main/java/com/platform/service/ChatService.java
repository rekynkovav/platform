package com.platform.service;

import com.platform.exception.ResourceNotFoundException;
import com.platform.model.MediaFile;
import com.platform.model.Message;
import com.platform.model.dto.MessageDto;
import com.platform.repository.MediaFileRepository;
import com.platform.repository.MessageRepository;
import com.platform.repository.ChatRoomPermissionRepository;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import io.micrometer.core.annotation.Timed;

import com.platform.model.ChatRoom;
import com.platform.repository.ChatRoomRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserService userService;
    private final MediaFileRepository mediaFileRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomService chatRoomService;
    private final ChatRoomPermissionRepository chatRoomPermissionRepository;
    // private final NotificationService notificationService;

    public ChatService(MessageRepository messageRepository,
                      UserService userService,
                      MediaFileRepository mediaFileRepository,
                      ChatRoomRepository chatRoomRepository,
                      ChatRoomService chatRoomService,
                      ChatRoomPermissionRepository chatRoomPermissionRepository) {
        this.messageRepository = messageRepository;
        this.userService = userService;
        this.mediaFileRepository = mediaFileRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomService = chatRoomService;
        this.chatRoomPermissionRepository = chatRoomPermissionRepository;
    }

    @Timed(value = "chat.get_general_messages", description = "Time to get general messages")
    @Transactional(readOnly = true, timeout = 5)
    public List<MessageDto> getGeneralMessages(int limit, String before) {
        try {
            log.debug("Getting general messages, limit: {}, before: {}", limit, before);
            Pageable pageable = PageRequest.of(0, limit);
            List<Message> messages;

            if (before != null && !before.isEmpty()) {
                Instant beforeInstant = Instant.parse(before);
                messages = messageRepository.findByChatTypeAndCreatedAtBefore(
                        "general", beforeInstant, pageable);
            } else {
                messages = messageRepository.findByChatTypeOrderByCreatedAtDesc("general", pageable);
            }

            log.debug("Retrieved {} general messages", messages.size());
            return messages.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());

        } catch (DateTimeParseException e) {
            log.error("Invalid date format for 'before' parameter: {}", before, e);
            throw new IllegalArgumentException("Некорректный формат даты: " + before, e);
        } catch (Exception e) {
            log.error("Error getting general messages: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при получении общих сообщений: " + e.getMessage(), e);
        }
    }

    @Timed(value = "chat.get_admin_messages", description = "Time to get admin group messages")
    public List<MessageDto> getAdminGroupMessages(int limit, String before) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Message> messages;

        if (before != null && !before.isEmpty()) {
            Instant beforeInstant = Instant.parse(before);
            messages = messageRepository.findByChatTypeAndCreatedAtBefore(
                    "admin_group", beforeInstant, pageable);
        } else {
            messages = messageRepository.findByChatTypeOrderByCreatedAtDesc("admin_group", pageable);
        }

        return messages.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Timed(value = "chat.get_private_messages", description = "Time to get private messages")
    public List<MessageDto> getPrivateMessages(String userId, String otherUserId, int limit, String before) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Message> messages;

        if (before != null && !before.isEmpty()) {
            Instant beforeInstant = Instant.parse(before);
            messages = messageRepository.findPrivateMessagesBefore(
                    userId, otherUserId, beforeInstant, pageable);
        } else {
            messages = messageRepository.findPrivateMessages(userId, otherUserId, pageable);
        }

        return messages.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Timed(value = "chat.send_general", description = "Time to send message to general chat")
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public MessageDto sendToGeneralChat(String userId, String content, String mediaId) {
        String userName = userService.getUserName(userId);

        // Получаем медиа данные заранее
        Optional<MediaFile> mediaOpt = getMediaFile(mediaId);

        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .userId(userId)
                .userName(userName)
                .chatType("general")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .mediaId(mediaOpt.map(m -> mediaId).orElse(null))
                .mediaType(mediaOpt.map(MediaFile::getFileType).orElse(null))
                .fileName(mediaOpt.map(MediaFile::getOriginalFilename).orElse(null))
                .build();

        message = messageRepository.save(message);
        log.info("Message sent to general chat: {} by {}, mediaId: {}", message.getId(), userId, mediaId);

        return toDto(message);
    }

    // Совместимость со старым API
    public MessageDto sendToGeneralChat(String userId, String content) {
        return sendToGeneralChat(userId, content, null);
    }

    @Timed(value = "chat.send_admin", description = "Time to send message to admin group")
    @Transactional
    public MessageDto sendToAdminGroup(String userId, String content, String mediaId) {
        String userName = userService.getUserName(userId);

        Optional<MediaFile> mediaOpt = getMediaFile(mediaId);

        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .userId(userId)
                .userName(userName)
                .chatType("admin_group")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .mediaId(mediaOpt.map(m -> mediaId).orElse(null))
                .mediaType(mediaOpt.map(MediaFile::getFileType).orElse(null))
                .fileName(mediaOpt.map(MediaFile::getOriginalFilename).orElse(null))
                .build();

        message = messageRepository.save(message);
        log.info("Message sent to admin group: {} by {}, mediaId: {}", message.getId(), userId, mediaId);

        return toDto(message);
    }

    // Совместимость со старым API
    public MessageDto sendToAdminGroup(String userId, String content) {
        return sendToAdminGroup(userId, content, null);
    }

    @Timed(value = "chat.send_private", description = "Time to send private message")
    @Transactional
    public MessageDto sendPrivateMessage(String fromUserId, String toUserId, String content, String mediaId) {
        String fromUserName = userService.getUserName(fromUserId);
        String toUserName = userService.getUserName(toUserId);

        Optional<MediaFile> mediaOpt = getMediaFile(mediaId);

        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .userId(fromUserId)
                .userName(fromUserName)
                .recipientId(toUserId)
                .chatType("private")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .mediaId(mediaOpt.map(m -> mediaId).orElse(null))
                .mediaType(mediaOpt.map(MediaFile::getFileType).orElse(null))
                .fileName(mediaOpt.map(MediaFile::getOriginalFilename).orElse(null))
                .build();

        message = messageRepository.save(message);
        log.info("Private message sent: {} -> {}, mediaId: {}", fromUserId, toUserId, mediaId);

        return toDto(message);
    }

    // Совместимость со старым API
    public MessageDto sendPrivateMessage(String fromUserId, String toUserId, String content) {
        return sendPrivateMessage(fromUserId, toUserId, content, null);
    }

    private Optional<MediaFile> getMediaFile(String mediaId) {
        if (mediaId == null || mediaId.isEmpty()) {
            return Optional.empty();
        }
        return mediaFileRepository.findById(mediaId);
    }

    @Transactional
    public void markAsRead(String messageId, String userId) {
        try {
            log.debug("Marking message as read: messageId: {}, userId: {}", messageId, userId);

            Message message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));

            if (message.getRecipientId() != null && message.getRecipientId().equals(userId)) {
                message.setIsRead(true);
                messageRepository.save(message);
                log.info("Message marked as read: {} by {}", messageId, userId);
            } else {
                log.warn("User {} is not the recipient of message {}", userId, messageId);
            }

        } catch (ResourceNotFoundException e) {
            log.error("Message not found for mark as read: {}", messageId, e);
            throw e;
        } catch (Exception e) {
            log.error("Error marking message {} as read by user {}: {}", messageId, userId, e.getMessage(), e);
            throw new RuntimeException("Ошибка при отметке сообщения как прочитанного: " + e.getMessage(), e);
        }
    }

    public long getUnreadCount(String userId) {
        return messageRepository.countUnreadByRecipient(userId);
    }

    @Timed(value = "chat.get_messages_paginated", description = "Time to get paginated messages")
    @Transactional(readOnly = true, timeout = 10)
    public Page<MessageDto.ProjectionDto> getMessages(String roomId, int page, int size) {
        UUID roomUuid = UUID.fromString(roomId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return messageRepository.findMessageDtosByRoomId(roomUuid, pageable);
    }

    @Timed(value = "chat.get_messages_cursor", description = "Time to get messages with cursor")
    @Transactional(readOnly = true, timeout = 5)
    public List<MessageDto> getMessagesAfter(String roomId, String cursor, int limit) {
        UUID roomUuid = UUID.fromString(roomId);
        UUID cursorUuid = cursor != null ? UUID.fromString(cursor) : null;
        Pageable pageable = PageRequest.of(0, limit);

        if (cursorUuid != null) {
            return messageRepository.findByRoomIdAndIdLessThanOrderByCreatedAtDesc(roomUuid, cursorUuid, pageable)
                .stream().map(this::toDto).collect(Collectors.toList());
        } else {
            return messageRepository.findByRoomId(roomUuid, pageable)
                .stream().map(this::toDto).collect(Collectors.toList());
        }
    }

    private MessageDto toDto(Message message) {
        return MessageDto.builder()
                .id(message.getId())
                .content(message.getContent())
                .userId(message.getUserId())
                .userName(message.getUserName())
                .chatType(message.getChatType())
                .recipientId(message.getRecipientId())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .mediaId(message.getMediaId())
                .mediaType(message.getMediaType())
                .fileName(message.getFileName())
                .roomId(message.getRoomId())
                .roomSlug(message.getRoomSlug())
                .build();
    }

    /**
     * Потолочный метод получения сообщений — с проверкой прав по роли пользователя
     */
    @Cacheable(value = "chatMessages", key = "#roomSlug + '_' + #limit + '_' + #before")
    public List<MessageDto> getMessagesByRoom(String roomSlug, int limit, Instant before, String userRole) {
        try {
            log.debug("Getting messages for room: {}, limit: {}, before: {}, userRole: {}",
                     roomSlug, limit, before, userRole);

            ChatRoom room = chatRoomRepository.findBySlug(roomSlug)
                    .orElseThrow(() -> new ResourceNotFoundException("Chat room not found: " + roomSlug));

            // Проверка прав
            if (!chatRoomService.hasPermission(room.getId(), userRole, "read")) {
                log.warn("Access denied for user role {} to room {}", userRole, roomSlug);
                throw new AccessDeniedException("Нет прав на чтение комнаты: " + roomSlug);
            }

            List<Message> messages;
            if (before == null) {
                messages = messageRepository.findByRoomIdOrderByCreatedAtDesc(room.getId(), PageRequest.of(0, limit));
            } else {
                messages = messageRepository.findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                        room.getId(), before, PageRequest.of(0, limit));
            }

            log.debug("Retrieved {} messages for room {}", messages.size(), roomSlug);
            return messages.stream().map(this::toDto).toList();

        } catch (ResourceNotFoundException e) {
            log.error("Chat room not found: {}", roomSlug, e);
            throw e;
        } catch (AccessDeniedException e) {
            log.error("Access denied for room {}: {}", roomSlug, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Error getting messages for room {}: {}", roomSlug, e.getMessage(), e);
            throw new RuntimeException("Ошибка при получении сообщений: " + e.getMessage(), e);
        }
    }

    /**
     * Потолочный метод отправки сообщения — с проверкой прав
     */
    @CacheEvict(value = {"chatMessages", "unreadNotifications", "allNotifications"}, allEntries = true)
    public MessageDto sendMessageToRoom(String roomSlug, String userId, String content, String mediaId, String userRole) {
        try {
            log.debug("Sending message to room: {}, userId: {}, content length: {}, mediaId: {}, userRole: {}",
                     roomSlug, userId, content != null ? content.length() : 0, mediaId, userRole);

            ChatRoom room = chatRoomRepository.findBySlug(roomSlug)
                    .orElseThrow(() -> new ResourceNotFoundException("Chat room not found: " + roomSlug));

            // Проверка прав на запись
            if (!chatRoomService.hasPermission(room.getId(), userRole, "read_write")) {
                log.warn("Write access denied for user role {} to room {}", userRole, roomSlug);
                throw new AccessDeniedException("Нет прав на отправку сообщений в комнату: " + roomSlug);
            }

            Message message = Message.builder()
                    .id(UUID.randomUUID())
                    .userId(UUID.fromString(userId))
                    .content(content != null ? content : "")
                    .mediaId(mediaId)
                    .roomId(room.getId())
                    .roomSlug(room.getSlug())
                    .build();

            Message saved = messageRepository.save(message);

            // === УВЕДОМЛЕНИЯ ДЛЯ ВКЛАДКИ ПОМОЩЬ ===
            // Временно отключено из-за циклической зависимости
            // if (room.getSlug().startsWith("questions-")) {
            //     notificationService.notifyAboutNewQuestionMessage(saved, room);
            // }

            log.debug("Message sent successfully to room {} with id {}", roomSlug, saved.getId());
            return toDto(saved);

        } catch (ResourceNotFoundException e) {
            log.error("Chat room not found for sending message: {}", roomSlug, e);
            throw e;
        } catch (AccessDeniedException e) {
            log.error("Access denied for sending message to room {}: {}", roomSlug, e.getMessage(), e);
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for userId {}: {}", userId, e.getMessage(), e);
            throw new IllegalArgumentException("Некорректный формат идентификатора пользователя: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error sending message to room {}: {}", roomSlug, e.getMessage(), e);
            throw new RuntimeException("Ошибка при отправке сообщения: " + e.getMessage(), e);
        }
    }
}