package com.platform.repository;

import com.platform.model.BroadcastStatus;
import com.platform.model.LiveBroadcast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LiveBroadcastRepository extends JpaRepository<LiveBroadcast, UUID> {

    List<LiveBroadcast> findByStatus(BroadcastStatus status);

    List<LiveBroadcast> findByOwnerId(String ownerId);

    Optional<LiveBroadcast> findByRtmpStreamKey(String streamKey);

    @Modifying
    @Query("UPDATE LiveBroadcast b SET b.viewerCount = b.viewerCount + 1 WHERE b.id = :id")
    void incrementViewerCount(@Param("id") String id);

    @Modifying
    @Query("UPDATE LiveBroadcast b SET b.viewerCount = b.viewerCount - 1 WHERE b.id = :id AND b.viewerCount > 0")
    void decrementViewerCount(@Param("id") String id);

    List<LiveBroadcast> findByStatusIn(List<BroadcastStatus> statuses);
}
