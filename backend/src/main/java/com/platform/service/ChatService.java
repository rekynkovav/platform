package com.platform.service;

import com.platform.exception.ResourceNotFoundException;
import com.platform.model.MediaFile;
import com.platform.model.Message;
import com.platform.model.dto.MessageDto;
import com.platform.model.dto.QuestionDto;
import com.platform.repository.MediaFileRepository;
import com.platform.repository.MessageRepository;
import com.platform.repository.ChatRoomPermissionRepository;
import org.springframework.security.access.AccessDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // ==================================================================
    // === LEGACY МЕТОДЫ (совместимость) ===
    // ==================================================================

    @Timed(value = "chat.get_general_messages", description = "Time to get general messages")
    @Transactional(readOnly = true, timeout = 5)
    public List<MessageDto> getGeneralMessages(int limit, String before) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            List<Message> messages;
            if (before != null && !before.isEmpty()) {
                Instant beforeInstant = Instant.parse(before);
                messages = messageRepository.findByChatTypeAndCreatedAtBefore("general", beforeInstant, pageable);
            } else {
                messages = messageRepository.findByChatTypeOrderByCreatedAtDesc("general", pageable);
            }
            return messages.stream().map(this::toDto).collect(Collectors.toList());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Некорректный формат даты: " + before, e);
        }
    }

    @Timed(value = "chat.get_admin_messages", description = "Time to get admin group messages")
    public List<MessageDto> getAdminGroupMessages(int limit, String before) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Message> messages;
        if (before != null && !before.isEmpty()) {
            Instant beforeInstant = Instant.parse(before);
            messages = messageRepository.findByChatTypeAndCreatedAtBefore("admin_group", beforeInstant, pageable);
        } else {
            messages = messageRepository.findByChatTypeOrderByCreatedAtDesc("admin_group", pageable);
        }
        return messages.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Timed(value = "chat.get_private_messages", description = "Time to get private messages")
    public List<MessageDto> getPrivateMessages(String userId, String otherUserId, int limit, String before) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Message> messages;
        if (before != null && !before.isEmpty()) {
            Instant beforeInstant = Instant.parse(before);
            messages = messageRepository.findPrivateMessagesBefore(UUID.fromString(userId), UUID.fromString(otherUserId), beforeInstant, pageable);
        } else {
            messages = messageRepository.findPrivateMessages(UUID.fromString(userId), UUID.fromString(otherUserId), pageable);
        }
        return messages.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Timed(value = "chat.send_general", description = "Time to send message to general chat")
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public MessageDto sendToGeneralChat(String userId, String content, String mediaId) {
        String userName = userService.getUserName(userId);
        Optional<MediaFile> mediaOpt = getMediaFile(mediaId);
        Message message = Message.builder()
                .id(UUID.randomUUID()).content(content).userId(UUID.fromString(userId)).userName(userName)
                .chatType("general").isRead(false).createdAt(LocalDateTime.now())
                .mediaId(mediaOpt.map(m -> mediaId).orElse(null))
                .mediaType(mediaOpt.map(MediaFile::getFileType).orElse(null))
                .fileName(mediaOpt.map(MediaFile::getOriginalFilename).orElse(null))
                .build();
        message = messageRepository.save(message);
        return toDto(message);
    }

    public MessageDto sendToGeneralChat(String userId, String content) {
        return sendToGeneralChat(userId, content, null);
    }

    @Timed(value = "chat.send_admin", description = "Time to send message to admin group")
    @Transactional
    public MessageDto sendToAdminGroup(String userId, String content, String mediaId) {
        String userName = userService.getUserName(userId);
        Optional<MediaFile> mediaOpt = getMediaFile(mediaId);
        Message message = Message.builder()
                .id(UUID.randomUUID()).content(content).userId(UUID.fromString(userId)).userName(userName)
                .chatType("admin_group").isRead(false).createdAt(LocalDateTime.now())
                .mediaId(mediaOpt.map(m -> mediaId).orElse(null))
                .mediaType(mediaOpt.map(MediaFile::getFileType).orElse(null))
                .fileName(mediaOpt.map(MediaFile::getOriginalFilename).orElse(null))
                .build();
        message = messageRepository.save(message);
        return toDto(message);
    }

    public MessageDto sendToAdminGroup(String userId, String content) {
        return sendToAdminGroup(userId, content, null);
    }

    @Timed(value = "chat.send_private", description = "Time to send private message")
    @Transactional
    public MessageDto sendPrivateMessage(String fromUserId, String toUserId, String content, String mediaId) {
        String fromUserName = userService.getUserName(fromUserId);
        Optional<MediaFile> mediaOpt = getMediaFile(mediaId);
        Message message = Message.builder()
                .id(UUID.randomUUID()).content(content).userId(UUID.fromString(fromUserId)).userName(fromUserName)
                .recipientId(UUID.fromString(toUserId)).chatType("private").isRead(false).createdAt(LocalDateTime.now())
                .mediaId(mediaOpt.map(m -> mediaId).orElse(null))
                .mediaType(mediaOpt.map(MediaFile::getFileType).orElse(null))
                .fileName(mediaOpt.map(MediaFile::getOriginalFilename).orElse(null))
                .build();
        message = messageRepository.save(message);
        return toDto(message);
    }

    public MessageDto sendPrivateMessage(String fromUserId, String toUserId, String content) {
        return sendPrivateMessage(fromUserId, toUserId, content, null);
    }

    @Transactional
    public void markAsRead(String messageId, String userId) {
        Message message = messageRepository.findById(UUID.fromString(messageId))
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));
        if (message.getRecipientId() != null && message.getRecipientId().equals(UUID.fromString(userId))) {
            message.setIsRead(true);
            messageRepository.save(message);
        }
    }

    public long getUnreadCount(String userId) {
        return messageRepository.countUnreadByRecipient(UUID.fromString(userId));
    }

    @Timed(value = "chat.get_messages_paginated", description = "Time to get paginated messages")
    @Transactional(readOnly = true, timeout = 10)
    public Page<MessageDto> getMessages(String roomId, int page, int size) {
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

    // ==================================================================
    // === ПОТОЛОЧНЫЕ МЕТОДЫ (room-based) ===
    // ==================================================================

    @Cacheable(value = "chatMessages", key = "#roomSlug + '_' + #limit + '_' + #before")
    public List<MessageDto> getMessagesByRoom(String roomSlug, int limit, Instant before, String userRole) {
        ChatRoom room = chatRoomRepository.findBySlug(roomSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found: " + roomSlug));
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

    @Timed(value = "chat.get_messages_by_room_paginated", description = "Time to get paginated messages by room")
    @Cacheable(value = "chatMessagesPaginated", key = "#roomSlug + '_' + #page + '_' + #size")
    public Page<MessageDto> getMessagesByRoomWithPagination(String roomSlug, int page, int size, String userRole) {
        ChatRoom room = chatRoomRepository.findBySlug(roomSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found: " + roomSlug));
        if (!chatRoomService.hasPermission(room.getId(), userRole, "read")) {
            throw new AccessDeniedException("Нет прав на чтение комнаты: " + roomSlug);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Message> messages = messageRepository.findByRoomId(room.getId(), pageable);
        return new PageImpl<>(messages.stream().map(this::toDto).collect(Collectors.toList()), pageable, messages.size());
    }

    @CacheEvict(value = {"chatMessages", "unreadNotifications", "allNotifications"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public MessageDto sendMessageToRoom(String roomSlug, String userId, String content, String mediaId, String userRole) {
        ChatRoom room = chatRoomRepository.findBySlug(roomSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found: " + roomSlug));
        if (!chatRoomService.hasPermission(room.getId(), userRole, "read_write")) {
            throw new AccessDeniedException("Нет прав на отправку сообщений в комнату: " + roomSlug);
        }
        String userName = userService.getUserName(userId);
        Message message = Message.builder()
                .id(UUID.randomUUID())
                .userId(UUID.fromString(userId))
                .userName(userName)
                .content(content != null ? content : "")
                .mediaId(mediaId)
                .roomId(room.getId())
                .roomSlug(room.getSlug())
                .chatType("room")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        Message saved = messageRepository.save(message);
        log.info("Message sent to room {} by user {}", roomSlug, userId);
        return toDto(saved);
    }

    // ==================================================================
    // === QUESTION DTO — ПОЛНАЯ РЕАЛИЗАЦИЯ ===
    // ==================================================================

    /**
     * Получить вопросы по режиму. Вопросы хранятся как сообщения в комнате questions-{mode}.
     * Ответы — как дочерние сообщения с recipientId = id вопроса.
     */
    @Cacheable(value = "questionsByMode", key = "#mode + '_' + #userId")
    public List<QuestionDto> getQuestionsByMode(String mode, String userId) {
        String roomSlug = "questions-" + mode;
        ChatRoom room = chatRoomRepository.findBySlug(roomSlug).orElse(null);
        if (room == null) {
            log.warn("Question room not found for mode: {}", mode);
            return List.of();
        }

        // Получаем все сообщения из комнаты (вопросы и ответы)
        List<Message> allMessages = messageRepository.findByRoomIdOrderByCreatedAtAsc(
                room.getId(), PageRequest.of(0, 100));

        // Строим индекс ответов по questionId для O(1) поиска
        Map<String, Message> answerMap = new HashMap<>();
        for (Message msg : allMessages) {
            if (msg.getRecipientId() != null) {
                answerMap.putIfAbsent(msg.getRecipientId().toString(), msg);
            }
        }

        // Формируем вопросы с ответами из Map
        List<QuestionDto> questions = new ArrayList<>();
        for (Message msg : allMessages) {
            if (msg.getRecipientId() == null) {
                QuestionDto q = QuestionDto.builder()
                        .id(msg.getId().toString())
                        .question(msg.getContent())
                        .authorId(msg.getUserId() != null ? msg.getUserId().toString() : null)
                        .authorName(msg.getUserName())
                        .mode(mode)
                        .isAnswered(false)
                        .createdAt(msg.getCreatedAt())
                        .build();

                Message answer = answerMap.get(q.getId());
                if (answer != null) {
                    q.setAnswer(answer.getContent());
                    q.setAnswered(true);
                    q.setAnsweredAt(answer.getCreatedAt());
                    q.setAnsweredBy(answer.getUserName());
                }
                questions.add(q);
            }
        }

        // Сортируем: неотвеченные сначала, затем по дате DESC
        questions.sort((a, b) -> {
            if (a.isAnswered() != b.isAnswered()) return a.isAnswered() ? 1 : -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        return questions;
    }

    /**
     * Создать вопрос в комнате questions-{mode}
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"chatMessages", "questionsByMode"}, allEntries = true)
    public QuestionDto createQuestion(String mode, QuestionDto.CreateRequest request, String userId) {
        String roomSlug = "questions-" + mode;
        ChatRoom room = chatRoomRepository.findBySlug(roomSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Комната вопросов не найдена: " + roomSlug));

        String userName = userService.getUserName(userId);

        Message message = Message.builder()
                .id(UUID.randomUUID())
                .userId(UUID.fromString(userId))
                .userName(userName)
                .content(request.getQuestion())
                .roomId(room.getId())
                .roomSlug(room.getSlug())
                .chatType("question")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Message saved = messageRepository.save(message);
        log.info("Question created in mode {} by user {}: {}", mode, userId, saved.getId());

        return QuestionDto.builder()
                .id(saved.getId().toString())
                .question(saved.getContent())
                .authorId(userId)
                .authorName(userName)
                .mode(mode)
                .isAnswered(false)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    /**
     * Ответить на вопрос. Создаёт сообщение с recipientId = questionId.
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"chatMessages", "questionsByMode"}, allEntries = true)
    public QuestionDto answerQuestion(String questionId, QuestionDto.AnswerRequest request, String adminId) {
        UUID questionUuid;
        try {
            questionUuid = UUID.fromString(questionId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Некорректный формат ID вопроса: " + questionId, e);
        }

        Message questionMsg = messageRepository.findById(questionUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Вопрос не найден: " + questionId));

        String adminName = userService.getUserName(adminId);

        // Создаём ответ как сообщение с recipientId = id вопроса
        Message answerMsg = Message.builder()
                .id(UUID.randomUUID())
                .userId(UUID.fromString(adminId))
                .userName(adminName)
                .content(request.getAnswer())
                .roomId(questionMsg.getRoomId())
                .roomSlug(questionMsg.getRoomSlug())
                .recipientId(questionUuid)
                .chatType("answer")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        messageRepository.save(answerMsg);
        log.info("Question {} answered by admin {}", questionId, adminId);

        return QuestionDto.builder()
                .id(questionId)
                .question(questionMsg.getContent())
                .authorId(questionMsg.getUserId() != null ? questionMsg.getUserId().toString() : null)
                .authorName(questionMsg.getUserName())
                .mode(extractModeFromSlug(questionMsg.getRoomSlug()))
                .isAnswered(true)
                .createdAt(questionMsg.getCreatedAt())
                .answeredAt(answerMsg.getCreatedAt())
                .answeredBy(adminName)
                .answer(request.getAnswer())
                .build();
    }

    /**
     * Удаление вопроса по ID. Проверяет права: автор или админ.
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"chatMessages", "questionsByMode"}, allEntries = true)
    public void deleteQuestion(String questionId, String userId) {
        UUID questionUuid;
        try {
            questionUuid = UUID.fromString(questionId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Некорректный формат ID вопроса: " + questionId, e);
        }

        Message message = messageRepository.findById(questionUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Вопрос не найден: " + questionId));

        boolean isAuthor = message.getUserId() != null && message.getUserId().toString().equals(userId);
        boolean isAdmin = userService.isAdmin(userId);

        if (!isAuthor && !isAdmin) {
            throw new AccessDeniedException("Нет прав на удаление этого вопроса");
        }

        // Удаляем также все ответы на этот вопрос
        messageRepository.deleteAnswersByRoomAndQuestionId(message.getRoomId(), questionUuid);

        messageRepository.delete(message);
        log.info("Question {} deleted by user {}", questionId, userId);
    }

    /**
     * Архивирование отвеченных вопросов старше 24 часов.
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"chatMessages", "questionsByMode"}, allEntries = true)
    public int archiveAnsweredQuestions(String mode, String userId) {
        String activeRoomSlug = "questions-" + mode;
        String archivedRoomSlug = activeRoomSlug + "-archived";

        ChatRoom room = chatRoomRepository.findBySlug(activeRoomSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Комната вопросов не найдена: " + activeRoomSlug));

        // Создаём архивную комнату если не существует
        chatRoomRepository.findBySlug(archivedRoomSlug).orElseGet(() -> {
            ChatRoom archivedRoom = ChatRoom.builder()
                    .slug(archivedRoomSlug)
                    .name(room.getName() + " (архив)")
                    .description("Архив отвеченных вопросов")
                    .build();
            return chatRoomRepository.save(archivedRoom);
        });

        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        int archivedCount = 0;
        int page = 0;
        int batchSize = 100;
        List<Message> batchMessages;

        do {
            batchMessages = messageRepository.findByRoomIdOrderByCreatedAtAsc(
                    room.getId(), PageRequest.of(page, batchSize));
            page++;

            for (Message msg : batchMessages) {
                if (msg.getRecipientId() == null && msg.getCreatedAt() != null && msg.getCreatedAt().isBefore(cutoff)) {
                    boolean hasAnswer = batchMessages.stream()
                            .anyMatch(m -> msg.getId().equals(m.getRecipientId()));
                    if (hasAnswer) {
                        msg.setRoomSlug(archivedRoomSlug);
                        messageRepository.save(msg);
                        for (Message answer : batchMessages) {
                            if (msg.getId().equals(answer.getRecipientId())) {
                                answer.setRoomSlug(archivedRoomSlug);
                                messageRepository.save(answer);
                            }
                        }
                        archivedCount++;
                    }
                }
            }
        } while (batchMessages.size() >= batchSize);

        log.info("Archived {} answered questions for mode {}", archivedCount, mode);
        return archivedCount;
    }

    // ==================================================================
    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===
    // ==================================================================

    private Optional<MediaFile> getMediaFile(String mediaId) {
        if (mediaId == null || mediaId.isEmpty()) return Optional.empty();
        return mediaFileRepository.findById(mediaId);
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

    private String extractModeFromSlug(String slug) {
        if (slug == null) return "general";
        // questions-general -> general, questions-nutrition -> nutrition
        if (slug.startsWith("questions-")) {
            String mode = slug.substring("questions-".length());
            // Убираем суффикс -archived если есть
            if (mode.endsWith("-archived")) {
                mode = mode.substring(0, mode.length() - "-archived".length());
            }
            return mode;
        }
        return "general";
    }
}
