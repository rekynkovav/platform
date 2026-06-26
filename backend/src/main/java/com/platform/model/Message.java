package com.platform.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(Message.MessageId.class)
public class Message {

    @Id
    private UUID id;

    @Id
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "chat_type")
    private String chatType;

    @Column(name = "recipient_id")
    private UUID recipientId;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "media_id")
    private String mediaId;

    @Column(name = "media_type")
    private String mediaType;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "room_slug", length = 100)
    private String roomSlug;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Composite primary key class for partitioned messages table.
     * Matches PRIMARY KEY (id, created_at) in v7 migration.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageId implements Serializable {
        private UUID id;
        private LocalDateTime createdAt;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MessageId messageId = (MessageId) o;
            return Objects.equals(id, messageId.id) && Objects.equals(createdAt, messageId.createdAt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, createdAt);
        }
    }
}
