package com.platform.service;

import com.platform.model.ChatRoom;
import com.platform.model.Message;
import com.platform.model.Notification;
import com.platform.repository.NotificationRepository;
import com.platform.websocket.ChatWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import org.springframework.data.domain.Pageable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private ChatWebSocketHandler webSocketHandler;
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;

    @InjectMocks
    private NotificationService notificationService;

    private String userId;
    private Notification notification;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        org.mockito.Mockito.lenient().when(cacheManager.getCache(anyString())).thenReturn(cache);
        notification = Notification.builder()
                .id(1L)
                .userId(userId)
                .type("new_message")
                .title("New Message")
                .message("Hello")
                .read(false)
                .createdAt(Instant.now())
                .build();
    }

    // ==================== createAndSend ====================

    @Test
    void createAndSend_SavesAndSendsViaWebSocket() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        Notification result = notificationService.createAndSend(
                userId, "new_message", "Title", "Body", "123", "general");

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getType()).isEqualTo("new_message");
        assertThat(result.getRead()).isFalse();
        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketHandler).sendNotificationToUser(eq(userId), any(Notification.class));
    }

    @Test
    void createAndSend_NullRelatedId_SetsNullLong() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(2L);
            return n;
        });

        Notification result = notificationService.createAndSend(
                userId, "info", "Info", "Text", null, null);

        assertThat(result.getRelatedId()).isNull();
        assertThat(result.getRelatedRoomSlug()).isNull();
    }

    // ==================== getUnreadForUser ====================

    @Test
    void getUnreadForUser_ReturnsUnreadList() {
        when(notificationRepository.findByUserIdAndReadFalse(eq(userId), any(Pageable.class)))
                .thenReturn(List.of(notification));

        List<Notification> result = notificationService.getUnreadForUser(userId, 50);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRead()).isFalse();
    }

    @Test
    void getUnreadForUser_NoUnread_ReturnsEmpty() {
        when(notificationRepository.findByUserIdAndReadFalse(eq(userId), any(Pageable.class)))
                .thenReturn(List.of());

        List<Notification> result = notificationService.getUnreadForUser(userId, 50);

        assertThat(result).isEmpty();
    }

    // ==================== getAllForUser ====================

    @Test
    void getAllForUser_ReturnsAllNotifications() {
        Notification read = Notification.builder()
                .id(2L).userId(userId).type("info").read(true).build();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(List.of(notification, read));

        List<Notification> result = notificationService.getAllForUser(userId, 50);

        assertThat(result).hasSize(2);
    }

    // ==================== markAsRead ====================

    @Test
    void markAsRead_Exists_MarksRead() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.markAsRead("1");

        assertThat(notification.getRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_NotFound_DoesNothing() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
                notificationService.markAsRead("999"));

        verify(notificationRepository, never()).save(any());
    }

    // ==================== markAsReadForUser ====================

    @Test
    void markAsReadForUser_OwnerCanMark() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.markAsReadForUser(1L, userId);

        assertThat(notification.getRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsReadForUser_NonOwner_ThrowsSecurityException() {
        String otherUserId = UUID.randomUUID().toString();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsReadForUser(1L, otherUserId))
                .isInstanceOf(SecurityException.class);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsReadForUser_NotFound_DoesNothing() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        notificationService.markAsReadForUser(999L, userId);

        verify(notificationRepository, never()).save(any());
    }

    // ==================== markAllAsRead ====================

    @Test
    void markAllAsRead_MarksAllUnreadAsRead() {
        notificationService.markAllAsRead(userId);

        verify(notificationRepository).markAllAsReadByUserId(userId);
        verify(cacheManager).getCache("unreadNotifications");
        verify(cacheManager).getCache("allNotifications");
    }

    @Test
    void markAllAsRead_NoUnread_SavesEmptyList() {
        notificationService.markAllAsRead(userId);

        verify(notificationRepository).markAllAsReadByUserId(userId);
    }

    // ==================== notifyAboutNewQuestionMessage ====================

    @Test
    void notifyAboutNewQuestionMessage_Question_CreatesNewQuestionNotification() {
        UUID msgId = UUID.randomUUID();
        Message msg = Message.builder()
                .id(msgId).userId(UUID.fromString(userId)).userName("User")
                .content("What is protein?").roomId(UUID.randomUUID())
                .build();
        ChatRoom room = ChatRoom.builder().slug("questions-nutrition").name("Q").build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(10L);
            return n;
        });

        notificationService.notifyAboutNewQuestionMessage(msg, room);

        verify(notificationRepository).save(argThat(n ->
                "new_question".equals(n.getType()) && "Новый вопрос".equals(n.getTitle())));
        verify(webSocketHandler).sendNotificationToUser(eq(userId), any(Notification.class));
    }

    @Test
    void notifyAboutNewQuestionMessage_Answer_CreatesNewAnswerNotification() {
        UUID msgId = UUID.randomUUID();
        Message msg = Message.builder()
                .id(msgId).userId(UUID.fromString(userId)).userName("Admin")
                .content("Ответ: Protein is essential.").roomId(UUID.randomUUID())
                .build();
        ChatRoom room = ChatRoom.builder().slug("questions-nutrition").name("Q").build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(11L);
            return n;
        });

        notificationService.notifyAboutNewQuestionMessage(msg, room);

        verify(notificationRepository).save(argThat(n ->
                "new_answer".equals(n.getType()) && "Получен ответ".equals(n.getTitle())));
    }

    @Test
    void notifyAboutNewQuestionMessage_LongContent_Truncates() {
        String longContent = "A".repeat(100);
        UUID msgId = UUID.randomUUID();
        Message msg = Message.builder()
                .id(msgId).userId(UUID.fromString(userId)).userName("User")
                .content(longContent).roomId(UUID.randomUUID())
                .build();
        ChatRoom room = ChatRoom.builder().slug("questions-general").name("Q").build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(12L);
            return n;
        });

        notificationService.notifyAboutNewQuestionMessage(msg, room);

        verify(notificationRepository).save(argThat(n ->
                n.getMessage() != null && n.getMessage().endsWith("...") && n.getMessage().length() == 80));
    }
}
