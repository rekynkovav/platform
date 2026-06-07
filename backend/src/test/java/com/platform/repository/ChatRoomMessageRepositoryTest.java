package com.platform.repository;

import com.platform.config.TestJpaConfig;
import com.platform.model.ChatRoom;
import com.platform.model.Message;
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
        messageRepository.save(Message.builder().content("First").userId(UUID.randomUUID()).roomId(roomId).version(0L).build());
        messageRepository.save(Message.builder().content("Second").userId(UUID.randomUUID()).roomId(roomId).version(0L).build());
        messageRepository.save(Message.builder().content("Other room").userId(UUID.randomUUID()).roomId(UUID.randomUUID()).version(0L).build());

        List<Message> messages = messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(0, 10));
        assertEquals(2, messages.size());
    }

    @Test
    void messageRepository_DeleteByRoomId_RemovesAllMessagesForRoom() {
        UUID roomId = UUID.randomUUID();
        messageRepository.save(Message.builder().content("Del1").userId(UUID.randomUUID()).roomId(roomId).version(0L).build());
        messageRepository.save(Message.builder().content("Del2").userId(UUID.randomUUID()).roomId(roomId).version(0L).build());
        UUID otherRoom = UUID.randomUUID();
        messageRepository.save(Message.builder().content("Keep").userId(UUID.randomUUID()).roomId(otherRoom).version(0L).build());

        messageRepository.deleteByRoomId(roomId);
        messageRepository.flush();

        assertEquals(0, messageRepository.findByRoomId(roomId, PageRequest.of(0, 100)).size());
        assertEquals(1, messageRepository.findByRoomId(otherRoom, PageRequest.of(0, 100)).size());
    }

    @Test
    void messageRepository_CountNewMessagesSince_CountsCorrectly() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);

        Message old = messageRepository.save(Message.builder().content("Old").userId(UUID.randomUUID()).version(0L).build());
        Message new1 = messageRepository.save(Message.builder().content("New1").userId(UUID.randomUUID()).version(0L).build());
        Message new2 = messageRepository.save(Message.builder().content("New2").userId(UUID.randomUUID()).version(0L).build());
        messageRepository.flush();

        // Обновляем created_at через JPQL, т.к. @CreationTimestamp + updatable=false
        messageRepository.updateCreatedAt(old.getId(), cutoff.minusHours(1));
        messageRepository.updateCreatedAt(new1.getId(), cutoff.plusMinutes(30));
        messageRepository.updateCreatedAt(new2.getId(), cutoff.plusMinutes(45));
        messageRepository.flush();

        long count = messageRepository.countNewMessagesSince(cutoff);
        assertEquals(2, count);
    }
}
