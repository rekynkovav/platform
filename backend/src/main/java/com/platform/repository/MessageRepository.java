package com.platform.repository;

import com.platform.model.Message;
import com.platform.model.dto.MessageDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT new com.platform.model.dto.MessageDto(m.id, m.content, m.createdAt, m.userName) " +
           "FROM Message m WHERE m.roomId = :roomId ORDER BY m.createdAt DESC")
    Page<MessageDto> findMessageDtosByRoomId(@Param("roomId") UUID roomId, Pageable pageable);

    @Query("SELECT new com.platform.model.dto.MessageDto(m.id, m.content, m.createdAt, m.userName) " +
           "FROM Message m WHERE m.roomId = :roomId AND m.id < :cursor ORDER BY m.createdAt DESC")
    Page<MessageDto> findMessageDtosByRoomIdAndCursor(@Param("roomId") UUID roomId, @Param("cursor") UUID cursor, Pageable pageable);

    @Query("SELECT new com.platform.model.dto.MessageDto(m.id, m.content, m.createdAt, m.userName) " +
           "FROM Message m WHERE m.roomId = :roomId AND m.createdAt > :after ORDER BY m.createdAt ASC")
    List<MessageDto> findNewMessagesSince(@Param("roomId") UUID roomId, @Param("after") LocalDateTime after);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.createdAt > :since")
    long countNewMessagesSince(@Param("since") LocalDateTime since);

    void deleteByRoomId(UUID roomId);

    @Query("SELECT m FROM Message m WHERE m.id = :id")
    Optional<Message> findByIdWithAuthor(@Param("id") UUID id);

    // Методы для ChatService
    List<Message> findByChatTypeAndCreatedAtBefore(String chatType, Instant before, Pageable pageable);
    List<Message> findByChatTypeOrderByCreatedAtDesc(String chatType, Pageable pageable);
    List<Message> findPrivateMessagesBefore(String userId, String otherUserId, Instant before, Pageable pageable);
    List<Message> findPrivateMessages(String userId, String otherUserId, Pageable pageable);
    long countUnreadByRecipient(String userId);
    List<Message> findByRoomIdOrderByCreatedAtDesc(UUID roomId, Pageable pageable);
    List<Message> findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(UUID roomId, Instant before, Pageable pageable);
    List<Message> findByRoomIdAndIdLessThanOrderByCreatedAtDesc(UUID roomId, UUID cursor, Pageable pageable);
    List<Message> findByRoomId(UUID roomId, Pageable pageable);

    // Метод для архивации старых сообщений
    List<Message> findByCreatedAtBefore(LocalDateTime cutoff);
}