package com.platform.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "chat_type")
    private String chatType; // "general", "admin_group", "private"

    @Column(name = "recipient_id")
    private UUID recipientId; // для личных сообщений

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "media_id")
    private String mediaId;

    @Column(name = "media_type")
    private String mediaType;

    @Column(name = "file_name")
    private String fileName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "room_slug", length = 100)
    private String roomSlug;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
}