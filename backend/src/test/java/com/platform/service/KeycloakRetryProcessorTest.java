package com.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.model.KeycloakRetryJob;
import com.platform.model.KeycloakRetryJob.Status;
import com.platform.repository.KeycloakRetryJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakRetryProcessorTest {

    @Mock private KeycloakRetryJobRepository retryJobRepository;
    @Mock private KeycloakService keycloakService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private KeycloakRetryProcessor keycloakRetryProcessor;

    @Test
    void enqueue_savesJobWithPendingStatus() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"userId\":\"u1\"}");

        keycloakRetryProcessor.enqueue("UPDATE_USER_ROLE", Map.of("userId", "u1"), "timeout");

        verify(retryJobRepository).save(argThat(job ->
            job.getOperationType().equals("UPDATE_USER_ROLE") &&
            job.getStatus() == Status.PENDING &&
            job.getAttemptCount() == 0
        ));
    }

    @Test
    void processRetryJobs_emptyList_doesNothing() {
        when(retryJobRepository.findRetryable(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        keycloakRetryProcessor.processRetryJobs();

        verify(keycloakService, never()).updateUserRole(any(), any(), any());
    }

    @Test
    void processRetryJobs_updateUserRole_callsKeycloak() throws Exception {
        KeycloakRetryJob job = KeycloakRetryJob.builder()
            .id(1L)
            .operationType("UPDATE_USER_ROLE")
            .payloadJson("{\"userId\":\"u1\",\"oldRole\":\"USER_1\",\"newRole\":\"USER_2\"}")
            .attemptCount(0)
            .maxAttempts(5)
            .status(Status.PENDING)
            .nextAttemptAt(LocalDateTime.now().minusMinutes(1))
            .build();

        when(retryJobRepository.findRetryable(any(LocalDateTime.class))).thenReturn(List.of(job));
        when(objectMapper.readValue(eq("{\"userId\":\"u1\",\"oldRole\":\"USER_1\",\"newRole\":\"USER_2\"}"), eq(Map.class)))
            .thenReturn(Map.of("userId", "u1", "oldRole", "USER_1", "newRole", "USER_2"));

        keycloakRetryProcessor.processRetryJobs();

        verify(keycloakService).updateUserRole("u1", "USER_1", "USER_2");
        assertThat(job.getStatus()).isEqualTo(Status.COMPLETED);
    }

    @Test
    void processRetryJobs_keycloakThrows_retriesWithBackoff() throws Exception {
        KeycloakRetryJob job = KeycloakRetryJob.builder()
            .id(2L)
            .operationType("ADD_USER_ROLE")
            .payloadJson("{\"userId\":\"u2\",\"roleName\":\"MODERATOR\"}")
            .attemptCount(0)
            .maxAttempts(5)
            .status(Status.PENDING)
            .nextAttemptAt(LocalDateTime.now().minusMinutes(1))
            .build();

        when(retryJobRepository.findRetryable(any(LocalDateTime.class))).thenReturn(List.of(job));
        when(objectMapper.readValue(anyString(), eq(Map.class)))
            .thenReturn(Map.of("userId", "u2", "roleName", "MODERATOR"));
        doThrow(new RuntimeException("Keycloak down")).when(keycloakService).addUserRole("u2", "MODERATOR");

        keycloakRetryProcessor.processRetryJobs();

        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(job.getStatus()).isEqualTo(Status.PENDING);
        assertThat(job.getLastError()).contains("Keycloak down");
    }

    @Test
    void getStats_returnsCountsByStatus() {
        when(retryJobRepository.countByStatus(Status.PENDING)).thenReturn(3L);
        when(retryJobRepository.countByStatus(Status.PROCESSING)).thenReturn(1L);
        when(retryJobRepository.countByStatus(Status.COMPLETED)).thenReturn(10L);
        when(retryJobRepository.countByStatus(Status.FAILED)).thenReturn(2L);

        Map<String, Object> stats = keycloakRetryProcessor.getStats();

        assertThat(stats).containsEntry("pending", 3L);
        assertThat(stats).containsEntry("completed", 10L);
    }
}
