package com.platform.repository;

import com.platform.model.entity.Message;
import com.platform.model.entity.User;
import com.platform.model.enums.ChatType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class MessageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MessageRepository messageRepository;

    private User user1;
    private User user2;
    private User user3;
    private Message message1;
    private Message message2;
    private Message message3;
    private Message message4;
    private Message message5;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        entityManager.clear();

        // Create test users
        user1 = User.builder()
                .id(UUID.randomUUID())
                .username("user1")
                .email("user1@example.com")
                .passwordHash("hash1")
                .build();

        user2 = User.builder()
                .id(UUID.randomUUID())
                .username("user2")
                .email("user2@example.com")
                .passwordHash("hash2")
                .build();

        user3 = User.builder()
                .id(UUID.randomUUID())
                .username("user3")
                .email("user3@example.com")
                .passwordHash("hash3")
                .build();

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.persist(user3);

        // Create test messages
        message1 = Message.builder()
                .id(UUID.randomUUID())
                .content("Hello from user1 to general chat")
                .sender(user1)
                .chatType(ChatType.GENERAL)
                .read(false)
                .createdAt(Instant.now().minusHours(5))
                .build();

        message2 = Message.builder()
                .id(UUID.randomUUID())
                .content("Private message from user1 to user2")
                .sender(user1)
                .recipient(user2)
                .chatType(ChatType.PRIVATE)
                .read(false)
                .createdAt(Instant.now().minusHours(3))
                .build();

        message3 = Message.builder()
                .id(UUID.randomUUID())
                .content("Private message from user2 to user1")
                .sender(user2)
                .recipient(user1)
                .chatType(ChatType.PRIVATE)
                .read(true)
                .createdAt(Instant.now().minusHours(2))
                .build();

        message4 = Message.builder()
                .id(UUID.randomUUID())
                .content("Another general message from user3")
                .sender(user3)
                .chatType(ChatType.GENERAL)
                .read(false)
                .createdAt(Instant.now().minusHours(1))
                .build();

        message5 = Message.builder()
                .id(UUID.randomUUID())
                .content("Private message from user1 to user3")
                .sender(user1)
                .recipient(user3)
                .chatType(ChatType.PRIVATE)
                .read(false)
                .createdAt(Instant.now().minusMinutes(30))
                .build();

        entityManager.persist(message1);
        entityManager.persist(message2);
        entityManager.persist(message3);
        entityManager.persist(message4);
        entityManager.persist(message5);
        entityManager.flush();
    }

    @Test
    void findBySenderId_ValidSender_ReturnsMessages() {
        // Act
        List<Message> messages = messageRepository.findBySenderId(user1.getId());

        // Assert
        assertThat(messages).hasSize(3); // message1, message2, message5
        assertThat(messages).extracting(Message::getContent)
                .containsExactlyInAnyOrder(
                        "Hello from user1 to general chat",
                        "Private message from user1 to user2",
                        "Private message from user1 to user3"
                );
    }

    @Test
    void findBySenderId_NonExistentSender_ReturnsEmptyList() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        List<Message> messages = messageRepository.findBySenderId(nonExistentId);

        // Assert
        assertThat(messages).isEmpty();
    }

    @Test
    void findByRecipientId_ValidRecipient_ReturnsMessages() {
        // Act
        List<Message> messages = messageRepository.findByRecipientId(user1.getId());

        // Assert
        assertThat(messages).hasSize(1); // message3
        assertThat(messages.get(0).getContent()).isEqualTo("Private message from user2 to user1");
        assertThat(messages.get(0).getSender().getId()).isEqualTo(user2.getId());
    }

    @Test
    void findByRecipientId_NoMessages_ReturnsEmptyList() {
        // Act
        List<Message> messages = messageRepository.findByRecipientId(user3.getId());

        // Assert
        assertThat(messages).isEmpty(); // user3 has no received messages
    }

    @Test
    void findByChatType_GeneralChat_ReturnsMessages() {
        // Act
        List<Message> messages = messageRepository.findByChatType(ChatType.GENERAL);

        // Assert
        assertThat(messages).hasSize(2); // message1, message4
        assertThat(messages).extracting(Message::getChatType)
                .containsOnly(ChatType.GENERAL);
    }

    @Test
    void findByChatType_PrivateChat_ReturnsMessages() {
        // Act
        List<Message> messages = messageRepository.findByChatType(ChatType.PRIVATE);

        // Assert
        assertThat(messages).hasSize(3); // message2, message3, message5
        assertThat(messages).extracting(Message::getChatType)
                .containsOnly(ChatType.PRIVATE);
    }

    @Test
    void findBySenderIdAndRecipientId_ValidPair_ReturnsMessages() {
        // Act
        List<Message> messages = messageRepository.findBySenderIdAndRecipientId(
                user1.getId(),
                user2.getId()
        );

        // Assert
        assertThat(messages).hasSize(1); // message2
        assertThat(messages.get(0).getContent()).isEqualTo("Private message from user1 to user2");
    }

    @Test
    void findBySenderIdAndRecipientId_ReversePair_ReturnsMessages() {
        // Act
        List<Message> messages = messageRepository.findBySenderIdAndRecipientId(
                user2.getId(),
                user1.getId()
        );

        // Assert
        assertThat(messages).hasSize(1); // message3
        assertThat(messages.get(0).getContent()).isEqualTo("Private message from user2 to user1");
    }

    @Test
    void findBySenderIdAndRecipientId_NoMessages_ReturnsEmptyList() {
        // Act
        List<Message> messages = messageRepository.findBySenderIdAndRecipientId(
                user2.getId(),
                user3.getId()
        );

        // Assert
        assertThat(messages).isEmpty();
    }

    @Test
    void findByReadFalse_ReturnsUnreadMessages() {
        // Act
        List<Message> unreadMessages = messageRepository.findByReadFalse();

        // Assert
        assertThat(unreadMessages).hasSize(3); // message1, message2, message4, message5 (message3 is read)
        assertThat(unreadMessages).extracting(Message::isRead)
                .containsOnly(false);
    }

    @Test
    void findByReadTrue_ReturnsReadMessages() {
        // Act
        List<Message> readMessages = messageRepository.findByReadTrue();

        // Assert
        assertThat(readMessages).hasSize(1); // message3
        assertThat(readMessages.get(0).isRead()).isTrue();
    }

    @Test
    void findByRecipientIdAndReadFalse_ValidRecipient_ReturnsUnreadMessages() {
        // Act
        List<Message> unreadMessages = messageRepository.findByRecipientIdAndReadFalse(user1.getId());

        // Assert
        assertThat(unreadMessages).isEmpty(); // user1 has no unread messages (message3 is read)
    }

    @Test
    void findByRecipientIdAndReadFalse_AnotherRecipient_ReturnsUnreadMessages() {
        // Act
        List<Message> unreadMessages = messageRepository.findByRecipientIdAndReadFalse(user2.getId());

        // Assert
        assertThat(unreadMessages).hasSize(1); // message2
        assertThat(unreadMessages.get(0).getContent()).isEqualTo("Private message from user1 to user2");
        assertThat(unreadMessages.get(0).isRead()).isFalse();
    }

    @Test
    void countByRecipientIdAndReadFalse_ValidRecipient_ReturnsCount() {
        // Act
        long unreadCount = messageRepository.countByRecipientIdAndReadFalse(user2.getId());

        // Assert
        assertThat(unreadCount).isEqualTo(1); // message2
    }

    @Test
    void countByRecipientIdAndReadFalse_NoUnread_ReturnsZero() {
        // Act
        long unreadCount = messageRepository.countByRecipientIdAndReadFalse(user1.getId());

        // Assert
        assertThat(unreadCount).isEqualTo(0);
    }

    @Test
    void findByCreatedAtBefore_ValidDate_ReturnsOlderMessages() {
        // Arrange
        Instant cutoff = Instant.now().minusHours(2);

        // Act
        List<Message> messages = messageRepository.findByCreatedAtBefore(cutoff);

        // Assert
        assertThat(messages).hasSize(2); // message1, message2
        assertThat(messages).extracting(Message::getContent)
                .containsExactlyInAnyOrder(
                        "Hello from user1 to general chat",
                        "Private message from user1 to user2"
                );
    }

    @Test
    void findByCreatedAtAfter_ValidDate_ReturnsNewerMessages() {
        // Arrange
        Instant cutoff = Instant.now().minusHours(2);

        // Act
        List<Message> messages = messageRepository.findByCreatedAtAfter(cutoff);

        // Assert
        assertThat(messages).hasSize(3); // message3, message4, message5
        assertThat(messages).extracting(Message::getContent)
                .containsExactlyInAnyOrder(
                        "Private message from user2 to user1",
                        "Another general message from user3",
                        "Private message from user1 to user3"
                );
    }

    @Test
    void findByCreatedAtBetween_ValidRange_ReturnsMessages() {
        // Arrange
        Instant start = Instant.now().minusHours(4);
        Instant end = Instant.now().minusHours(1);

        // Act
        List<Message> messages = messageRepository.findByCreatedAtBetween(start, end);

        // Assert
        assertThat(messages).hasSize(2); // message2, message3
        assertThat(messages).extracting(Message::getContent)
                .containsExactlyInAnyOrder(
                        "Private message from user1 to user2",
                        "Private message from user2 to user1"
                );
    }

    @Test
    void findAllByOrderByCreatedAtDesc_ReturnsSortedMessages() {
        // Act
        List<Message> messages = messageRepository.findAllByOrderByCreatedAtDesc();

        // Assert
        assertThat(messages).hasSize(5);
        // Should be sorted by createdAt descending (newest first)
        assertThat(messages.get(0).getContent()).isEqualTo("Private message from user1 to user3"); // 30 min ago
        assertThat(messages.get(1).getContent()).isEqualTo("Another general message from user3"); // 1 hour ago
        assertThat(messages.get(2).getContent()).isEqualTo("Private message from user2 to user1"); // 2 hours ago
        assertThat(messages.get(3).getContent()).isEqualTo("Private message from user1 to user2"); // 3 hours ago
        assertThat(messages.get(4).getContent()).isEqualTo("Hello from user1 to general chat"); // 5 hours ago
    }

    @Test
    void markAsRead_ValidMessage_UpdatesStatus() {
        // Act
        int updatedCount = messageRepository.markAsRead(message2.getId());
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(updatedCount).isEqualTo(1);
        Message updatedMessage = entityManager.find(Message.class, message2.getId());
        assertThat(updatedMessage.isRead()).isTrue();
    }

    @Test
    void markAsRead_NonExistentMessage_ReturnsZero() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        int updatedCount = messageRepository.markAsRead(nonExistentId);

        // Assert
        assertThat(updatedCount).isEqualTo(0);
    }

    @Test
    void markAllAsReadByRecipient_ValidRecipient_UpdatesMessages() {
        // Act
        int updatedCount = messageRepository.markAllAsReadByRecipient(user2.getId());
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(updatedCount).isEqualTo(1); // message2
        Message updatedMessage = entityManager.find(Message.class, message2.getId());
        assertThat(updatedMessage.isRead()).isTrue();
    }

    @Test
    void markAllAsReadByRecipient_NoMessages_ReturnsZero() {
        // Act
        int updatedCount = messageRepository.markAllAsReadByRecipient(user3.getId());

        // Assert
        assertThat(updatedCount).isEqualTo(0);
    }

    @Test
    void deleteBySenderId_ValidSender_DeletesMessages() {
        // Act
        long deletedCount = messageRepository.deleteBySenderId(user1.getId());
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(deletedCount).isEqualTo(3); // message1, message2, message5
        List<Message> remainingMessages = messageRepository.findAll();
        assertThat(remainingMessages).hasSize(2); // message3, message4
    }

    @Test
    void deleteBySenderId_NonExistentSender_ReturnsZero() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        long deletedCount = messageRepository.deleteBySenderId(nonExistentId);

        // Assert
        assertThat(deletedCount).isEqualTo(0);
    }

    @Test
    void deleteByRecipientId_ValidRecipient_DeletesMessages() {
        // Act
        long deletedCount = messageRepository.deleteByRecipientId(user1.getId());
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(deletedCount).isEqualTo(1); // message3
        List<Message> remainingMessages = messageRepository.findAll();
        assertThat(remainingMessages).hasSize(4); // message1, message2, message4, message5
    }

    @Test
    void findGeneralMessages_ReturnsGeneralChatMessages() {
        // Act
        Page<Message> page = messageRepository.findGeneralMessages(PageRequest.of(0, 10));

        // Assert
        assertThat(page.getTotalElements()).isEqualTo(2); // message1, message4
        assertThat(page.getContent()).extracting(Message::getChatType)
                .containsOnly(ChatType.GENERAL);
    }

    @Test
    void findGeneralMessages_WithPagination_ReturnsPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Act
        Page<Message> page = messageRepository.findGeneralMessages(pageable);

        // Assert
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent().get(0).getContent()).isEqualTo("Another general message from user3"); // Newest
    }

    @Test
    void findPrivateMessagesBetweenUsers_ValidUsers_ReturnsMessages() {
        // Act
        List<Message> messages = messageRepository.findPrivateMessagesBetweenUsers(
                user1.getId(),
                user2.getId()
        );

        // Assert
        assertThat(messages).hasSize(2); // message2, message3
        assertThat(messages).extracting(Message::getContent)
                .containsExactlyInAnyOrder(
                        "Private message from user1 to user2",
                        "Private message from user2 to user1"
                );
    }

    @Test
    void findPrivateMessagesBetweenUsers_NoMessages_ReturnsEmptyList() {
        // Act
        List<Message> messages = messageRepository.findPrivateMessagesBetweenUsers(
                user2.getId(),
                user3.getId()
        );

        // Assert
        assertThat(messages).isEmpty();
    }

    @Test
    void findPrivateMessagesBetweenUsers_WithPagination_ReturnsMessages() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Act
        Page<Message> page = messageRepository.findPrivateMessagesBetweenUsers(
                user1.getId(),
                user2.getId(),
                pageable
        );

        // Assert
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getContent()).isEqualTo("Private message from user2 to user1"); // Newest
    }

    @Test
    void countByChatType_GeneralChat_ReturnsCount() {
        // Act
        long count = messageRepository.countByChatType(ChatType.GENERAL);

        // Assert
        assertThat(count).isEqualTo(2); // message1, message4
    }

    @Test
    void countByChatType_PrivateChat_ReturnsCount() {
        // Act
        long count = messageRepository.countByChatType(ChatType.PRIVATE);

        // Assert
        assertThat(count).isEqualTo(3); // message2, message3, message5
    }

    @Test
    void countBySenderId_ValidSender_ReturnsCount() {
        // Act
        long count = messageRepository.countBySenderId(user1.getId());

        // Assert
        assertThat(count).isEqualTo(3); // message1, message2, message5
    }

    @Test
    void countByRecipientId_ValidRecipient_ReturnsCount() {
        // Act
        long count = messageRepository.countByRecipientId(user1.getId());

        // Assert
        assertThat(count).isEqualTo(1); // message3
    }
}