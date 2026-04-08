package com.platform.repository;

import com.platform.model.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    List<AccessLog> findByFileIdOrderByAccessedAtDesc(String fileId);

    List<AccessLog> findByUserIdOrderByAccessedAtDesc(String userId);

    @Query("SELECT a FROM AccessLog a WHERE a.fileId = :fileId AND a.accessedAt > :since")
    List<AccessLog> findRecentAccessByFile(@Param("fileId") String fileId, @Param("since") LocalDateTime since);

    @Query("SELECT a FROM AccessLog a WHERE a.userId = :userId AND a.action = :action AND a.accessedAt > :since")
    List<AccessLog> findUserActionsSince(@Param("userId") String userId, @Param("action") String action, @Param("since") LocalDateTime since);

    long countByFileIdAndAction(String fileId, String action);
}
