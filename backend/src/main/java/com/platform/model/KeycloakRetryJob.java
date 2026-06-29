package com.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "keycloak_retry_jobs", indexes = {
    @Index(name = "idx_retry_status", columnList = "status"),
    @Index(name = "idx_retry_next_attempt", columnList = "nextAttemptAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakRetryJob {

    public enum Status { PENDING, PROCESSING, COMPLETED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String operationType;

    @Column(nullable = false, length = 2000)
    private String payloadJson;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private int maxAttempts;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column
    private String lastError;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (status == null) status = Status.PENDING;
        if (attemptCount == 0) attemptCount = 0;
        if (maxAttempts == 0) maxAttempts = 5;
        if (nextAttemptAt == null) nextAttemptAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
