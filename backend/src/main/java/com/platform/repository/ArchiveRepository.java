package com.platform.repository;

import com.platform.model.Archive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ArchiveRepository extends JpaRepository<Archive, String> {

    List<Archive> findAllByOrderByStartTimeDesc(Pageable pageable);

    @Query("SELECT a FROM Archive a WHERE a.startTime < :before ORDER BY a.startTime DESC")
    List<Archive> findByStartTimeBefore(@Param("before") String before, Pageable pageable);

    List<Archive> findByEventId(String eventId);

    @Modifying
    @Transactional
    @Query("UPDATE Archive a SET a.viewsCount = a.viewsCount + 1 WHERE a.id = :id")
    void incrementViews(@Param("id") String id);
}
