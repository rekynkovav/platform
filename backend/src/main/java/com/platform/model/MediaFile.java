package com.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "encryption_iv", nullable = false)
    private String encryptionIv;

    @Column(name = "file_type", nullable = false)
    private String fileType; // video, audio, voice, image

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    private Integer duration; // для видео/аудио в секундах

    @Column(name = "watermark_data", columnDefinition = "TEXT")
    private String watermarkData; // JSON: {userId, timestamp, position}

    @Column(name = "access_level")
    private String accessLevel; // public, premium, private

    @Column(name = "hls_playlist_url")
    private String hlsPlaylistUrl; // для HLS стриминга

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "views_count")
    private Integer viewsCount = 0;

    @Column(name = "download_count")
    private Integer downloadCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
