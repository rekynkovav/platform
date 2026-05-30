package com.platform.service;

import com.platform.exception.SecurityException;
import com.platform.model.Notification;
import com.platform.model.dto.NotificationDto;
import com.platform.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ChatWebSocketHandler webSocketHandler;

    @InjectMocks
    private NotificationService notificationService;

    private String userId;
    private UUID userIdUuid;
    private String notificationId;
    private UUID notificationIdUuid;
    private String otherUserId;
    private UUID otherUserIdUuid;

    @BeforeEach
    void setUp() {
        userId = "user-123";
        userIdUuid = UUID.fromString(userId);
        notificationId = "notification-456";
        notificationIdUuid = UUID.fromString(notificationId);
        otherUserId = "other-789";
        otherUserIdUuid = UUID.fromString(otherUserId);
    }

    @Test
    void createAndSend_SavesAndSendsNotification() {
        // Arrange
        String type = "new_message";
        String content = "You have a new message";
        String relatedId = "message-123";

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification notification = inv.getArgument(0);
            notification.setId(notificationIdUuid);
            return notification;
        });

        // Act
        notificationService.createAndSend(userId, type, content, relatedId);

        // Assert
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getUserId()).isEqualTo(userIdUuid);
        assertThat(savedNotification.getType()).isEqualTo(type);
        assertThat(savedNotification.getContent()).isEqualTo(content);
        assertThat(savedNotification.getRelatedId()).isEqualTo(relatedId);
        assertThat(savedNotification.isRead()).isFalse();

        verify(webSocketHandler).sendNotificationToUser(userId, any(NotificationDto.class));
    }

    @Test
    void getUnreadForUser_ReturnsUnreadNotifications() {
        // Arrange
        Notification unread1 = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userIdUuid)
                .type("new_message")
                .content("Message 1")
                .read(false)
                .build();

        Notification unread2 = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userIdUuid)
                .type("new_question")
                .content("Question 1")
                .read(false)
                .build();

        when(notificationRepository.findByUserIdAndRead(userIdUuid, false))
                .thenReturn(List.of(unread1, unread2));

        // Act
        List<NotificationDto> result = notificationService.getUnreadForUser(userId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).isRead()).isFalse();
        assertThat(result.get(1).isRead()).isFalse();
        verify(notificationRepository).findByUserIdAndRead(userIdUuid, false);
    }

    @Test
    void markAsReadForUser_Owner_MarksAsRead() {
        // Arrange
        Notification notification = Notification.builder()
                .id(notificationIdUuid)
                .userId(userIdUuid)
                .type("new_message")
                .content("Test notification")
                .read(false)
                .build();

        when(notificationRepository.findById(notificationIdUuid)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // Act
        notificationService.markAsReadForUser(notificationId, userId);

        // Assert
        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsReadForUser_NotOwner_ThrowsException() {
        // Arrange
        Notification notification = Notification.builder()
                .id(notificationIdUuid)
                .userId(userIdUuid)  // Belongs to userId
                .type("new_message")
                .content("Test notification")
                .read(false)
                .build();

        when(notificationRepository.findById(notificationIdUuid)).thenReturn(Optional.of(notification));

        // Act & Assert
        assertThatThrownBy(() -> notificationService.markAsReadForUser(notificationId, otherUserId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("not authorized");

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void markAllAsRead_MarksAllUnreadAsRead() {
        // Arrange
        Notification unread1 = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userIdUuid)
                .type("new_message")
                .content("Message 1")
                .read(false)
                .build();

        Notification unread2 = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userIdUuid)
                .type("new_question")
                .content("Question 1")
                .read(false)
                .build();

        when(notificationRepository.findByUserIdAndRead(userIdUuid, false))
                .thenReturn(List.of(unread1, unread2));
        when(notificationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        notificationService.markAllAsRead(userId);

        // Assert
        assertThat(unread1.isRead()).isTrue();
        assertThat(unread2.isRead()).isTrue();
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    void notifyAboutNewQuestionMessage_QuestionMessage_SetsTypeNewQuestion() {
        // Arrange
        String questionId = "question-123";
        String questionContent = "How to implement this feature?";
        String senderName = "QuestionAuthor";

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification notification = inv.getArgument(0);
            notification.setId(notificationIdUuid);
            return notification;
        });

        // Act
        notificationService.notifyAboutNewQuestionMessage(userId, questionId, questionContent, senderName);

        // Assert
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getType()).isEqualTo("new_question");
        assertThat(savedNotification.getContent()).contains("How to implement this feature?");
        assertThat(savedNotification.getContent()).contains("QuestionAuthor");
        assertThat(savedNotification.getRelatedId()).isEqualTo(questionId);

        verify(webSocketHandler).sendNotificationToUser(userId, any(NotificationDto.class));
    }

    @Test
    void notifyAboutNewQuestionMessage_AnswerMessage_SetsTypeNewAnswer() {
        // Arrange
        String questionId = "question-123";
        String answerContent = "Ответ: You need to use the following approach...";
        String senderName = "AnswerAuthor";

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification notification = inv.getArgument(0);
            notification.setId(notificationIdUuid);
            return notification;
        });

        // Act
        notificationService.notifyAboutNewQuestionMessage(userId, questionId, answerContent, senderName);

        // Assert
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getType()).isEqualTo("new_answer");
        assertThat(savedNotification.getContent()).contains("Ответ: You need to use");
        assertThat(savedNotification.getContent()).contains("AnswerAuthor");

        verify(webSocketHandler).sendNotificationToUser(userId, any(NotificationDto.class));
    }

    @Test
    void notifyAboutNewQuestionMessage_LongContent_TruncatesTo80Chars() {
        // Arrange
        String questionId = "question-123";
        String longContent = "This is a very long question content that exceeds eighty characters and needs to be truncated for the notification preview.";
        String senderName = "LongQuestionAuthor";

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification notification = inv.getArgument(0);
            notification.setId(notificationIdUuid);
            return notification;
        });

        // Act
        notificationService.notifyAboutNewQuestionMessage(userId, questionId, longContent, senderName);

        // Assert
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        String notificationContent = savedNotification.getContent();
        assertThat(notificationContent.length()).isLessThanOrEqualTo(80 + senderName.length() + 10); // Account for sender name
        assertThat(notificationContent).contains("...");

        verify(webSocketHandler).sendNotificationToUser(userId, any(NotificationDto.class));
    }

    @Test
    void markAsRead_Exists_MarksAsRead() {
        // Arrange
        Notification notification = Notification.builder()
                .id(notificationIdUuid)
                .userId(userIdUuid)
                .type("new_message")
                .content("Test notification")
                .read(false)
                .build();

        when(notificationRepository.findById(notificationIdUuid)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // Act
        notificationService.markAsRead(notificationId);

        // Assert
        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_NotFound_DoesNothing() {
        // Arrange
        String nonExistentNotificationId = "non-existent-notification";
        UUID nonExistentUuid = UUID.fromString(nonExistentNotificationId);
        when(notificationRepository.findById(nonExistentUuid)).thenReturn(Optional.empty());

        // Act
        notificationService.markAsRead(nonExistentNotificationId);

        // Assert
        verify(notificationRepository, never()).save(any(Notification.class));
    }
}