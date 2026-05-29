package com.platform.service;

import com.platform.model.ChatRoom;
import com.platform.model.Message;
import com.platform.model.Notification;
import com.platform.repository.NotificationRepository;
import com.platform.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ChatWebSocketHandler webSocketHandler;

    /**
     * Создать уведомление и сразу отправить по WebSocket
     */
    public Notification createAndSend(String userId, String type, String title, String message,
                                      String relatedId, String relatedRoomSlug) {

        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .relatedId(relatedId)
                .relatedRoomSlug(relatedRoomSlug)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        // Отправляем по WebSocket конкретному пользователю
        webSocketHandler.sendNotificationToUser(userId, saved);

        return saved;
    }

    /**
     * Получить непрочитанные уведомления пользователя
     */
    @Cacheable(value = "unreadNotifications", key = "#userId")
    public List<Notification> getUnreadForUser(String userId) {
        return notificationRepository.findByUserIdAndReadFalse(userId);
    }

    /**
     * Получить все уведомления пользователя (для колокольчика)
     */
    @Cacheable(value = "allNotifications", key = "#userId")
    public List<Notification> getAllForUser(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Пометить как прочитанное
     */
    @CacheEvict(value = {"unreadNotifications", "allNotifications"}, key = "#notification.userId")
    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    /**
     * Пометить уведомление как прочитанное с проверкой владельца
     */
    @CacheEvict(value = {"unreadNotifications", "allNotifications"}, key = "#userId")
    public void markAsReadForUser(Long notificationId, String userId) {
        notificationRepository.findById(notificationId.toString()).ifPresent(n -> {
            if (!n.getUserId().equals(userId)) {
                throw new SecurityException("Access denied to notification");
            }
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    /**
     * Пометить все как прочитанные
     */
    @CacheEvict(value = {"unreadNotifications", "allNotifications"}, key = "#userId")
    public void markAllAsRead(String userId) {
        List<Notification> unread = getUnreadForUser(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    /**
     * Финальная версия: создаём уведомление при новом сообщении в комнате вопросов
     */
    public void notifyAboutNewQuestionMessage(Message savedMessage, ChatRoom room) {
        boolean isAnswer = savedMessage.getContent() != null && savedMessage.getContent().startsWith("Ответ:");

        String type = isAnswer ? "new_answer" : "new_question";
        String title = isAnswer ? "Получен ответ" : "Новый вопрос";
        String shortMessage = savedMessage.getContent() != null && savedMessage.getContent().length() > 80
                ? savedMessage.getContent().substring(0, 77) + "..."
                : savedMessage.getContent();

        createAndSend(
                savedMessage.getUserId(),
                type,
                title,
                shortMessage,
                savedMessage.getId(),
                room.getSlug()
        );
    }
}