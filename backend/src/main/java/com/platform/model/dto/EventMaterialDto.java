package com.platform.model.dto;

import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class EventMaterialDto {
    private UUID id;
    private String sectionId;
    private String type;
    private String title;
    private String content;
    private String url;
    private String mediaId;
    private String createdBy;
    private String authorName;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Section ID is required")
        private String sectionId;

        @NotBlank(message = "Type is required")
        private String type;

        @NotBlank(message = "Title is required")
        @Size(max = 500)
        private String title;

        private String content;
        private String url;
        private String mediaId;
        private Integer sortOrder;
    }

    @Data
    public static class UpdateRequest {
        private String title;
        private String content;
        private String url;
        private String mediaId;
        private Integer sortOrder;
        private Boolean isActive;
    }
}
