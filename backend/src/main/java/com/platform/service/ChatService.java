package com.platform.service;

import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Message;
import com.platform.model.dto.MessageDto;
import com.platform.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserService userService;

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
    public MessageDto sendToGeneralChat(String userId, String content) {
        String userName = userService.getUserName(userId);

        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .userId(userId)
                .userName(userName)
                .chatType("general")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        message = messageRepository.save(message);
        log.info("Message sent to general chat: {} by {}", message.getId(), userId);

        return toDto(message);
    }

    @Transactional
    public MessageDto sendToAdminGroup(String userId, String content) {
        String userName = userService.getUserName(userId);

        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .userId(userId)
                .userName(userName)
                .chatType("admin_group")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        message = messageRepository.save(message);
        log.info("Message sent to admin group: {} by {}", message.getId(), userId);

        return toDto(message);
    }

    @Transactional
    public MessageDto sendPrivateMessage(String fromUserId, String toUserId, String content) {
        String fromUserName = userService.getUserName(fromUserId);
        String toUserName = userService.getUserName(toUserId);

        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .userId(fromUserId)
                .userName(fromUserName)
                .recipientId(toUserId)
                .chatType("private")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        message = messageRepository.save(message);
        log.info("Private message sent: {} -> {}", fromUserId, toUserId);

        return toDto(message);
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
                .build();
    }
}