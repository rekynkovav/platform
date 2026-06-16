package com.platform.repository;

import com.platform.config.TestJpaConfig;
import com.platform.model.ChatRoom;
import com.platform.model.Message;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(TestJpaConfig.class)
@ActiveProfiles("test")
class ChatRoomMessageRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private EntityManager entityManager;

    private Message buildMessage(String content, UUID roomId) {
        return Message.builder()
                .id(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .content(content)
                .userId(UUID.randomUUID())
                .roomId(roomId)
                .version(0L)
                .build();
    }

    // ==================== ChatRoomRepository ====================

    @Test
    void chatRoomRepository_FindBySlug_ReturnsRoom() {
        ChatRoom room = ChatRoom.builder()
                .slug("general")
                .type("general")
                .name("General Chat")
                .isActive(true)
                .build();
        chatRoomRepository.save(room);

        Optional<ChatRoom> found = chatRoomRepository.findBySlug("general");
        assertTrue(found.isPresent());
        assertEquals("General Chat", found.get().getName());
    }

    @Test
    void chatRoomRepository_ExistsBySlug_Works() {
        chatRoomRepository.save(ChatRoom.builder().slug("nutrition").type("general").name("Nutrition").isActive(true).build());

        assertTrue(chatRoomRepository.existsBySlug("nutrition"));
        assertFalse(chatRoomRepository.existsBySlug("nonexistent"));
    }

    // ==================== MessageRepository ====================

    @Test
    void messageRepository_FindByRoomIdOrderByCreatedAtDesc_ReturnsOrdered() {
        UUID roomId = UUID.randomUUID();
        messageRepository.save(buildMessage("First", roomId));
        messageRepository.flush();
        entityManager.clear();

        messageRepository.save(buildMessage("Second", roomId));
        messageRepository.flush();
        entityManager.clear();

        messageRepository.save(buildMessage("Other room", UUID.randomUUID()));
        messageRepository.flush();
        entityManager.clear();

        List<Message> messages = messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(0, 10));
        assertEquals(2, messages.size());
    }

    @Test
    void messageRepository_DeleteByRoomId_RemovesAllMessagesForRoom() {
        UUID roomId = UUID.randomUUID();
        messageRepository.save(buildMessage("Del1", roomId));
        messageRepository.save(buildMessage("Del2", roomId));
        UUID otherRoom = UUID.randomUUID();
        messageRepository.save(buildMessage("Keep", otherRoom));
        messageRepository.flush();
        entityManager.clear();

        messageRepository.deleteByRoomId(roomId);
        messageRepository.flush();
        entityManager.clear();

        assertEquals(0, messageRepository.findByRoomId(roomId, PageRequest.of(0, 100)).size());
        assertEquals(1, messageRepository.findByRoomId(otherRoom, PageRequest.of(0, 100)).size());
    }

    @Test
    void messageRepository_CountNewMessagesSince_CountsCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(30);

        // Create messages that will be "old" (createdAt before cutoff)
        Message old1 = Message.builder()
                .id(UUID.randomUUID()).createdAt(cutoff.minusHours(1))
                .content("Old1").userId(UUID.randomUUID()).version(0L).build();
        Message old2 = Message.builder()
                .id(UUID.randomUUID()).createdAt(cutoff.minusHours(2))
                .content("Old2").userId(UUID.randomUUID()).version(0L).build();
        messageRepository.save(old1);
        messageRepository.save(old2);
        messageRepository.flush();
        entityManager.clear();

        // Create messages that will be "new" (createdAt after cutoff)
        Message new1 = Message.builder()
                .id(UUID.randomUUID()).createdAt(now)
                .content("New1").userId(UUID.randomUUID()).version(0L).build();
        Message new2 = Message.builder()
                .id(UUID.randomUUID()).createdAt(now)
                .content("New2").userId(UUID.randomUUID()).version(0L).build();
        messageRepository.save(new1);
        messageRepository.save(new2);
        messageRepository.flush();
        entityManager.clear();

        long count = messageRepository.countNewMessagesSince(cutoff);
        assertEquals(2, count);
    }
}
