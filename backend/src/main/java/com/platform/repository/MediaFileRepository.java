package com.platform.repository;

import com.platform.model.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, String> {

    List<MediaFile> findByOwnerId(String ownerId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE MediaFile m SET m.viewsCount = m.viewsCount + 1 WHERE m.id = :id")
    void incrementViews(@Param("id") String id);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE MediaFile m SET m.downloadCount = m.downloadCount + 1 WHERE m.id = :id")
    void incrementDownloads(@Param("id") String id);

    @Query("SELECT m FROM MediaFile m WHERE m.fileType = 'HLS' AND m.createdAt < :cutoff")
    List<MediaFile> findOldHlsSegments(@Param("cutoff") LocalDateTime cutoff);
}
