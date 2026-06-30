package com.platform.repository;

import com.platform.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, String> {

    @Query("SELECT e FROM Event e WHERE e.startTime > :now ORDER BY e.startTime ASC")
    List<Event> findUpcoming(LocalDateTime now);

    @Query("SELECT e FROM Event e WHERE e.startTime < :now ORDER BY e.startTime DESC")
    List<Event> findPast(LocalDateTime now, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.startTime > :now ORDER BY e.startTime ASC LIMIT 1")
    Optional<Event> findNextEvent(LocalDateTime now);

    List<Event> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
}
