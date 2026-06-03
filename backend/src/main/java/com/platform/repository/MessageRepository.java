package com.platform.repository;

import com.platform.model.Message;
import com.platform.model.dto.MessageDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    @Query("SELECT m FROM Message m WHERE m.chatType = 'private' " +
           "AND ((m.userId = :userId AND m.recipientId = :otherUserId) OR (m.userId = :otherUserId AND m.recipientId = :userId)) " +
           "AND m.createdAt < :before ORDER BY m.createdAt DESC")
    List<Message> findPrivateMessagesBefore(@Param("userId") String userId, @Param("otherUserId") String otherUserId,
                                            @Param("before") Instant before, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.chatType = 'private' " +
           "AND ((m.userId = :userId AND m.recipientId = :otherUserId) OR (m.userId = :otherUserId AND m.recipientId = :userId)) " +
           "ORDER BY m.createdAt DESC")
    List<Message> findPrivateMessages(@Param("userId") String userId, @Param("otherUserId") String otherUserId,
                                      Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipientId = :userId AND m.isRead = false")
    long countUnreadByRecipient(@Param("userId") String userId);

    List<Message> findByRoomIdOrderByCreatedAtDesc(UUID roomId, Pageable pageable);
    List<Message> findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(UUID roomId, Instant before, Pageable pageable);
    List<Message> findByRoomIdAndIdLessThanOrderByCreatedAtDesc(UUID roomId, UUID cursor, Pageable pageable);
    List<Message> findByRoomId(UUID roomId, Pageable pageable);

    // Метод для архивации старых сообщений
    List<Message> findByCreatedAtBefore(LocalDateTime cutoff);

    // Метод для получения сообщений по комнате с сортировкой ASC (для архивации)
    List<Message> findByRoomIdOrderByCreatedAtAsc(UUID roomId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Message m SET m.createdAt = :createdAt WHERE m.id = :id")
    void updateCreatedAt(@Param("id") UUID id, @Param("createdAt") LocalDateTime createdAt);
}
