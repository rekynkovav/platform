package com.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "media_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaFile {

    @Id
    private String id;

    @Version
    @Builder.Default
    private Long version = 0L;

    @Column(nullable = false)
    private String ownerId;

    @Column(nullable = false)
    private String s3Key;

    @Column(nullable = false)
    private String encryptionIv;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private String originalFilename;

    private String contentType;

    private Long fileSize;

    private Integer duration;

    @Column(length = 1000)
    private String watermarkData;

    private String hlsPlaylistUrl;

    private String accessLevel;

    private Integer viewsCount;

    private Integer downloadCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (viewsCount == null) viewsCount = 0;
        if (downloadCount == null) downloadCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}