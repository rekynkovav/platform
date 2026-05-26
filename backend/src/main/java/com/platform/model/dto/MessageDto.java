package com.platform.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageDto {
    private UUID id;
    private String type;
    private String content;
    private UUID userId;
    private String userName;
    private String chatType;
    private UUID recipientId;
    private Boolean isRead;
    private LocalDateTime createdAt;

    // Медиа поля
    private String mediaId;
    private String mediaType;
    private String fileName;
    private String mediaUrl;

    private UUID roomId;
    private String roomSlug;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendRequest {
        private String content;
        private UUID recipientId;
        private String chatType;
        private String mediaId;
    }

    // Внутренний DTO для проекции JPQL
    @Value
    public static class ProjectionDto {
        UUID id;
        String content;
        LocalDateTime createdAt;
        String authorName;

        // Конструктор для проекции JPQL
        public ProjectionDto(UUID id, String content, LocalDateTime createdAt, String authorName) {
            this.id = id;
            this.content = content;
            this.createdAt = createdAt;
            this.authorName = authorName;
        }
    }
}