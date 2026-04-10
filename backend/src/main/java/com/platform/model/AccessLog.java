package com.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "media_access_logs", indexes = {
    @Index(name = "idx_access_file_id", columnList = "file_id"),
    @Index(name = "idx_access_user_id", columnList = "user_id"),
    @Index(name = "idx_access_timestamp", columnList = "accessed_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_id", nullable = false)
    private String fileId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "action", nullable = false)
    private String action; // STREAM_REQUEST, KEY_REQUEST, DOWNLOAD_ATTEMPT, VIEW

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "success")
    private Boolean success = true;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "accessed_at", updatable = false)
    private LocalDateTime accessedAt;
}
