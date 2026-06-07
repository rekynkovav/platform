package com.platform.repository;

import com.platform.config.TestJpaConfig;
import com.platform.model.Archive;
import com.platform.model.MediaFile;
import com.platform.model.Notification;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(TestJpaConfig.class)
@ActiveProfiles("test")
class MediaArchiveNotificationRepositoryTest {

    @Autowired
    private MediaFileRepository mediaFileRepository;

    @Autowired
    private ArchiveRepository archiveRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EntityManager entityManager;

    // ==================== MediaFileRepository ====================

    @Test
    void mediaFileRepository_FindByOwnerId_ReturnsUserFiles() {
        mediaFileRepository.save(MediaFile.builder().id("m1").ownerId("user-1").s3Key("k1").encryptionIv("iv1").fileType("image").originalFilename("a.jpg").build());
        mediaFileRepository.save(MediaFile.builder().id("m2").ownerId("user-1").s3Key("k2").encryptionIv("iv2").fileType("video").originalFilename("b.mp4").build());
        mediaFileRepository.save(MediaFile.builder().id("m3").ownerId("user-2").s3Key("k3").encryptionIv("iv3").fileType("image").originalFilename("c.jpg").build());

        List<MediaFile> files = mediaFileRepository.findByOwnerId("user-1");
        assertEquals(2, files.size());
    }

    @Test
    void mediaFileRepository_IncrementViews_IncrementsCount() {
        MediaFile file = MediaFile.builder().id("mv1").ownerId("u1").s3Key("k").encryptionIv("iv").fileType("video").originalFilename("v.mp4").viewsCount(10).build();
        mediaFileRepository.save(file);
        mediaFileRepository.flush();

        mediaFileRepository.incrementViews("mv1");
        mediaFileRepository.flush();
        entityManager.clear();

        MediaFile updated = mediaFileRepository.findById("mv1").orElseThrow();
        assertEquals(11, updated.getViewsCount());
    }

    @Test
    void mediaFileRepository_IncrementDownloads_IncrementsCount() {
        MediaFile file = MediaFile.builder().id("md1").ownerId("u1").s3Key("k").encryptionIv("iv").fileType("doc").originalFilename("d.pdf").downloadCount(5).build();
        mediaFileRepository.save(file);
        mediaFileRepository.flush();

        mediaFileRepository.incrementDownloads("md1");
        mediaFileRepository.flush();
        entityManager.clear();

        MediaFile updated = mediaFileRepository.findById("md1").orElseThrow();
        assertEquals(6, updated.getDownloadCount());
    }

    // ==================== ArchiveRepository ====================

    @Test
    void archiveRepository_FindByEventId_ReturnsMatchingArchives() {
        archiveRepository.save(Archive.builder().id("a1").title("A1").eventId("ev1").startTime(LocalDateTime.now()).build());
        archiveRepository.save(Archive.builder().id("a2").title("A2").eventId("ev1").startTime(LocalDateTime.now()).build());
        archiveRepository.save(Archive.builder().id("a3").title("A3").eventId("ev2").startTime(LocalDateTime.now()).build());

        List<Archive> archives = archiveRepository.findByEventId("ev1");
        assertEquals(2, archives.size());
    }

    @Test
    void archiveRepository_IncrementViews_IncrementsCount() {
        archiveRepository.save(Archive.builder().id("av1").title("View test").eventId("ev1").viewsCount(20).startTime(LocalDateTime.now()).build());
        archiveRepository.flush();

        archiveRepository.incrementViews("av1");
        archiveRepository.flush();
        entityManager.clear();

        Archive updated = archiveRepository.findById("av1").orElseThrow();
        assertEquals(21, updated.getViewsCount());
    }

    // ==================== NotificationRepository ====================

    @Test
    void notificationRepository_FindByUserIdAndReadFalse_ReturnsUnreadOnly() {
        notificationRepository.save(Notification.builder().userId("u1").message("Unread1").read(false).build());
        notificationRepository.save(Notification.builder().userId("u1").message("Read1").read(true).build());
        notificationRepository.save(Notification.builder().userId("u1").message("Unread2").read(false).build());

        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse("u1");
        assertEquals(2, unread.size());
        assertTrue(unread.stream().noneMatch(n -> Boolean.TRUE.equals(n.getRead())));
    }
}
