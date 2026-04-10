package com.platform.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaFileDto {

    private String id;
    private String ownerId;
    private String fileType;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private Integer duration;
    private String accessLevel;
    private String thumbnailUrl;
    private Integer viewsCount;
    private Integer downloadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadResponse {
        private String fileId;
        private String url;
        private String type;
        private Long size;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreamResponse {
        private String url;
        private Integer expiresIn;
        private String type;
    }
}
