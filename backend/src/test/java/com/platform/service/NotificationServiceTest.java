package com.platform.service;

import com.platform.model.Notification;
import com.platform.repository.NotificationRepository;
import com.platform.websocket.ChatWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ChatWebSocketHandler webSocketHandler;

    @InjectMocks
    private NotificationService notificationService;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id("test-id")
                .userId("user-123")
                .type("info")
                .title("Test Title")
                .message("Test Message")
                .read(false)
                .build();
    }

    @Test
    void testCreateAndSend_ValidData_CreatesAndSendsNotification() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        Notification result = notificationService.createAndSend(
                "user-123", "info", "Test Title", "Test Message", "related-123", "room-slug");

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("user-123");
        assertThat(result.getType()).isEqualTo("info");
        assertThat(result.getTitle()).isEqualTo("Test Title");
        assertThat(result.getMessage()).isEqualTo("Test Message");

        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketHandler).sendNotificationToUser(eq("user-123"), eq(result));
    }

    @Test
    void testGetUnreadForUser_ReturnsUnreadNotifications() {
        List<Notification> unreadNotifications = List.of(testNotification);
        when(notificationRepository.findByUserIdAndReadFalse("user-123")).thenReturn(unreadNotifications);

        List<Notification> result = notificationService.getUnreadForUser("user-123");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isRead()).isFalse();
        verify(notificationRepository).findByUserIdAndReadFalse("user-123");
    }

    @Test
    void testGetAllForUser_ReturnsAllNotifications() {
        List<Notification> allNotifications = List.of(testNotification);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("user-123")).thenReturn(allNotifications);

        List<Notification> result = notificationService.getAllForUser("user-123");

        assertThat(result).hasSize(1);
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc("user-123");
    }

    @Test
    void testMarkAsRead_MarksNotificationAsRead() {
        when(notificationRepository.findById("test-id")).thenReturn(Optional.of(testNotification));

        notificationService.markAsRead("test-id");

        assertThat(testNotification.isRead()).isTrue();
        verify(notificationRepository).save(testNotification);
    }

    @Test
    void testMarkAsReadForUser_WithCorrectUser_MarksAsRead() {
        when(notificationRepository.findById("123")).thenReturn(Optional.of(testNotification));

        notificationService.markAsReadForUser(123L, "user-123");

        assertThat(testNotification.isRead()).isTrue();
        verify(notificationRepository).save(testNotification);
    }

    @Test
    void testMarkAsReadForUser_WithWrongUser_ThrowsSecurityException() {
        when(notificationRepository.findById("123")).thenReturn(Optional.of(testNotification));

        assertThatThrownBy(() -> notificationService.markAsReadForUser(123L, "wrong-user"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Access denied");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void testMarkAllAsRead_MarksAllUnreadAsRead() {
        List<Notification> unreadNotifications = List.of(
                Notification.builder().id("1").userId("user-123").read(false).build(),
                Notification.builder().id("2").userId("user-123").read(false).build()
        );

        when(notificationRepository.findByUserIdAndReadFalse("user-123")).thenReturn(unreadNotifications);

        notificationService.markAllAsRead("user-123");

        assertThat(unreadNotifications).allMatch(Notification::isRead);
        verify(notificationRepository).saveAll(unreadNotifications);
    }

    @Test
    void testNotifyAboutNewQuestionMessage_WithQuestion_CreatesQuestionNotification() {
        com.platform.model.Message message = com.platform.model.Message.builder()
                .id("msg-123")
                .userId("user-123")
                .content("This is a question about the platform")
                .build();

        com.platform.model.ChatRoom room = com.platform.model.ChatRoom.builder()
                .slug("questions-room")
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.notifyAboutNewQuestionMessage(message, room);

        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketHandler).sendNotificationToUser(eq("user-123"), any(Notification.class));
    }

    @Test
    void testNotifyAboutNewQuestionMessage_WithAnswer_CreatesAnswerNotification() {
        com.platform.model.Message message = com.platform.model.Message.builder()
                .id("msg-123")
                .userId("user-123")
                .content("Ответ: This is an answer to the question")
                .build();

        com.platform.model.ChatRoom room = com.platform.model.ChatRoom.builder()
                .slug("questions-room")
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.notifyAboutNewQuestionMessage(message, room);

        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketHandler).sendNotificationToUser(eq("user-123"), any(Notification.class));
    }
}