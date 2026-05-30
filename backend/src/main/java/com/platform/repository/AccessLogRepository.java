package com.platform.repository;

import com.platform.model.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    List<AccessLog> findByFileId(String fileId);

    List<AccessLog> findByUserId(String userId);
}