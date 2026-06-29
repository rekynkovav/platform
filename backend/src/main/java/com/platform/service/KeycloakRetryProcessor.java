package com.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.model.KeycloakRetryJob;
import com.platform.model.KeycloakRetryJob.Status;
import com.platform.repository.KeycloakRetryJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakRetryProcessor {

    private final KeycloakRetryJobRepository retryJobRepository;
    private final KeycloakService keycloakService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void enqueue(String operationType, Map<String, String> payload, String lastError) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            KeycloakRetryJob job = KeycloakRetryJob.builder()
                .operationType(operationType)
                .payloadJson(json)
                .attemptCount(0)
                .maxAttempts(5)
                .status(Status.PENDING)
                .nextAttemptAt(LocalDateTime.now())
                .lastError(lastError)
                .build();

            retryJobRepository.save(job);
            log.warn("Enqueued Keycloak retry job: type={}, error={}", operationType, lastError);
        } catch (Exception e) {
            log.error("Failed to enqueue Keycloak retry job", e);
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    @Transactional
    public void processRetryJobs() {
        List<KeycloakRetryJob> jobs = retryJobRepository.findRetryable(LocalDateTime.now());
        if (jobs.isEmpty()) return;

        log.info("Processing {} Keycloak retry jobs", jobs.size());

        for (KeycloakRetryJob job : jobs) {
            job.setStatus(Status.PROCESSING);
            retryJobRepository.save(job);

            try {
                @SuppressWarnings("unchecked")
                Map<String, String> payload = objectMapper.readValue(job.getPayloadJson(), Map.class);

                switch (job.getOperationType()) {
                    case "UPDATE_USER_ROLE" -> {
                        keycloakService.updateUserRole(
                            payload.get("userId"),
                            payload.get("oldRole"),
                            payload.get("newRole")
                        );
                        job.setStatus(Status.COMPLETED);
                        log.info("Retry job {} completed: UPDATE_USER_ROLE for {}", job.getId(), payload.get("userId"));
                    }
                    case "ADD_USER_ROLE" -> {
                        keycloakService.addUserRole(
                            payload.get("userId"),
                            payload.get("roleName")
                        );
                        job.setStatus(Status.COMPLETED);
                        log.info("Retry job {} completed: ADD_USER_ROLE for {}", job.getId(), payload.get("userId"));
                    }
                    default -> {
                        log.warn("Unknown retry operation type: {}", job.getOperationType());
                        job.setStatus(Status.FAILED);
                        job.setLastError("Unknown operation type: " + job.getOperationType());
                    }
                }
            } catch (Exception e) {
                job.setAttemptCount(job.getAttemptCount() + 1);
                job.setLastError(e.getMessage());
                job.setNextAttemptAt(LocalDateTime.now().plusSeconds(
                    (long) Math.pow(2, job.getAttemptCount()) * 30
                ));

                if (job.getAttemptCount() >= job.getMaxAttempts()) {
                    job.setStatus(Status.FAILED);
                    log.error("Retry job {} exhausted all {} attempts: {}",
                        job.getId(), job.getMaxAttempts(), e.getMessage());
                } else {
                    job.setStatus(Status.PENDING);
                    log.warn("Retry job {} attempt {}/{} failed, next retry at {}: {}",
                        job.getId(), job.getAttemptCount(), job.getMaxAttempts(),
                        job.getNextAttemptAt(), e.getMessage());
                }
            }
            retryJobRepository.save(job);
        }
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", retryJobRepository.countByStatus(Status.PENDING));
        stats.put("processing", retryJobRepository.countByStatus(Status.PROCESSING));
        stats.put("completed", retryJobRepository.countByStatus(Status.COMPLETED));
        stats.put("failed", retryJobRepository.countByStatus(Status.FAILED));
        return stats;
    }
}
