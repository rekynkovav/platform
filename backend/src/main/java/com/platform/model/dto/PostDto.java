// src/main/java/com/platform/model/dto/PostDto.java
package com.platform.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostDto {
    private String id;
    private String content;
    private String authorId;
    private String authorName;
    private String authorRole;
    private String mediaUrl;
    private String mediaType;
    private String category;
    private Integer likesCount;
    private Integer commentsCount;
    private Integer sharesCount;
    private Integer viewsCount;
    private Boolean userLiked;
    private Boolean isPinned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentDto> recentComments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String content;
        private String mediaUrl;
        private String mediaType;
        private String category;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String content;
        private String mediaUrl;
        private String mediaType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaUploadResponse {
        private String url;
        private String type;
        private Long size;
        private String filename;
    }
}
