package com.platform.repository;

import com.platform.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    List<Message> findByChatTypeOrderByCreatedAtDesc(String chatType, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.chatType = :chatType AND m.createdAt < :before ORDER BY m.createdAt DESC")
    List<Message> findByChatTypeAndCreatedAtBefore(@Param("chatType") String chatType,
                                                   @Param("before") String before,
                                                   Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.chatType = 'private' AND " +
            "((m.userId = :userId AND m.recipientId = :otherId) OR " +
            "(m.userId = :otherId AND m.recipientId = :userId)) " +
            "ORDER BY m.createdAt DESC")
    List<Message> findPrivateMessages(@Param("userId") String userId,
                                      @Param("otherId") String otherId,
                                      Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.chatType = 'private' AND " +
            "((m.userId = :userId AND m.recipientId = :otherId) OR " +
            "(m.userId = :otherId AND m.recipientId = :userId)) AND " +
            "m.createdAt < :before ORDER BY m.createdAt DESC")
    List<Message> findPrivateMessagesBefore(@Param("userId") String userId,
                                            @Param("otherId") String otherId,
                                            @Param("before") String before,
                                            Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipientId = :userId AND m.isRead = false")
    long countUnreadByRecipient(@Param("userId") String userId);
}