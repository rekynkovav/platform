// src/main/java/com/platform/model/dto/MessageDto.java
package com.platform.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageDto {
    private String id;
    private String type;
    private String content;
    private String userId;
    private String userName;
    private String chatType;
    private String recipientId;
    private Boolean isRead;
    private LocalDateTime createdAt;

    // Медиа поля
    private String mediaId;
    private String mediaType;
    private String fileName;
    private String mediaUrl;

    private Long roomId;
    private String roomSlug;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendRequest {
        private String content;
        private String recipientId;
        private String chatType;
        private String mediaId;
    }
}