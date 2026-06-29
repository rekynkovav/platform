package com.platform.repository;

import com.platform.model.KeycloakRetryJob;
import com.platform.model.KeycloakRetryJob.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KeycloakRetryJobRepository extends JpaRepository<KeycloakRetryJob, Long> {

    @Query("SELECT j FROM KeycloakRetryJob j WHERE j.status IN ('PENDING', 'FAILED') " +
           "AND j.nextAttemptAt <= :now AND j.attemptCount < j.maxAttempts " +
           "ORDER BY j.nextAttemptAt ASC")
    List<KeycloakRetryJob> findRetryable(LocalDateTime now);

    long countByStatus(Status status);
}
