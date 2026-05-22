package com.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "live_broadcasts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveBroadcast {

    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String streamType;

    @Column(nullable = false)
    private String ownerId;

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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        viewerCount = 0;
        maxViewers = 0;
        isPublic = true;
        isChatEnabled = true;
        isReactionsEnabled = true;
        isRecording = false;
        if (status == null) {
            status = BroadcastStatus.IDLE;
        }
    }
}
