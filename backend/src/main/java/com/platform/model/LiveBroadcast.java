package com.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_broadcasts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveBroadcast {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String streamType;

    @Column(nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BroadcastStatus status;

    @Column
    private Integer viewerCount;

    @Column
    private Integer maxViewers;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime endedAt;

    @Column
    private String hlsPlaylistUrl;

    @Column
    private String rtmpStreamKey;

    @Column(name = "recorded_url")
    private String recordedUrl;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    @Column(name = "recorded_size")
    private Long recordedSize;

    @Column(name = "recorded_duration")
    private Integer recordedDuration;

    @Column
    private Boolean isPublic;

    @Column
    private Boolean isChatEnabled;

    @Column
    private Boolean isReactionsEnabled;

    @Column
    private Boolean isRecording;

    @Column
    private LocalDateTime createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (viewerCount == null) viewerCount = 0;
        if (maxViewers == null) maxViewers = 0;
        if (isPublic == null) isPublic = true;
        if (isChatEnabled == null) isChatEnabled = true;
        if (isReactionsEnabled == null) isReactionsEnabled = true;
        if (isRecording == null) isRecording = false;
        if (status == null) status = BroadcastStatus.IDLE;
    }
}
