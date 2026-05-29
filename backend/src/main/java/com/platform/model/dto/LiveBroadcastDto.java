package com.platform.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LiveBroadcastDto {
    private String id;
    private String title;
    private String description;
    private String streamType;
    private String userId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer viewerCount;
    private String hlsPlaylistUrl;
    private String thumbnailUrl;

    // Безопасные поля для клиента
    // НЕ включаем: streamKey, internalStreamUrl, encryptionKey, rtmpUrl
}