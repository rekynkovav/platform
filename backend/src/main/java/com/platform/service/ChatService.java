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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import com.platform.model.ChatRoom;
import com.platform.repository.ChatRoomRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserService userService;
    private final MediaFileRepository mediaFileRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomService chatRoomService;
    private final ChatRoomPermissionRepository chatRoomPermissionRepository;
    private final NotificationService notificationService;

    public List<MessageDto> getGeneralMessages(int limit, String before) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Message> messages;

        if (before != null && !before.isEmpty()) {
            messages = messageRepository.findByChatTypeAndCreatedAtBefore(
                    "general", before, pageable);
        } else {
            messages = messageRepository.findByChatTypeOrderByCreatedAtDesc("general", pageable);
        }

        return messages.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<MessageDto> getAdminGroupMessages(int limit, String before) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Message> messages;

        if (before != null && !before.isEmpty()) {
            messages = messageRepository.findByChatTypeAndCreatedAtBefore(
                    "admin_group", before, pageable);
        } else {
            messages = messageRepository.findByChatTypeOrderByCreatedAtDesc("admin_group", pageable);
        }

        return messages.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<MessageDto> getPrivateMessages(String userId, String otherUserId, int limit, String before) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Message> messages;

        if (before != null && !before.isEmpty()) {
            messages = messageRepository.findPrivateMessagesBefore(
                    userId, otherUserId, before, pageable);
        } else {
            messages = messageRepository.findPrivateMessages(userId, otherUserId, pageable);
        }

        return messages.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
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
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));

        if (message.getRecipientId() != null && message.getRecipientId().equals(userId)) {
            message.setIsRead(true);
            messageRepository.save(message);
            log.info("Message marked as read: {} by {}", messageId, userId);
        }
    }

    public long getUnreadCount(String userId) {
        return messageRepository.countUnreadByRecipient(userId);
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
        ChatRoom room = chatRoomRepository.findBySlug(roomSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found: " + roomSlug));

        // Проверка прав
        if (!chatRoomService.hasPermission(room.getId(), userRole, "read")) {
            throw new AccessDeniedException("Нет прав на чтение комнаты: " + roomSlug);
        }

        List<Message> messages;
        if (before == null) {
            messages = messageRepository.findByRoomIdOrderByCreatedAtDesc(room.getId(), PageRequest.of(0, limit));
        } else {
            messages = messageRepository.findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                    room.getId(), before, PageRequest.of(0, limit));
        }
        return messages.stream().map(this::toDto).toList();
    }

    /**
     * Потолочный метод отправки сообщения — с проверкой прав
     */
    @CacheEvict(value = {"chatMessages", "unreadNotifications", "allNotifications"}, allEntries = true)
    public MessageDto sendMessageToRoom(String roomSlug, String userId, String content, String mediaId, String userRole) {
        ChatRoom room = chatRoomRepository.findBySlug(roomSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found: " + roomSlug));

        // Проверка прав на запись
        if (!chatRoomService.hasPermission(room.getId(), userRole, "read_write")) {
            throw new AccessDeniedException("Нет прав на отправку сообщений в комнату: " + roomSlug);
        }

        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .content(content != null ? content : "")
                .mediaId(mediaId)
                .roomId(room.getId())
                .roomSlug(room.getSlug())
                .build();

        Message saved = messageRepository.save(message);

        // === УВЕДОМЛЕНИЯ ДЛЯ ВКЛАДКИ ПОМОЩЬ ===
        if (room.getSlug().startsWith("questions-")) {
            notificationService.notifyAboutNewQuestionMessage(saved, room);
        }

        return toDto(saved);
    }
}