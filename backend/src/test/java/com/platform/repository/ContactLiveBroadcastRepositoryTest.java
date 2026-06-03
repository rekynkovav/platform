package com.platform.repository;

import com.platform.config.TestJpaConfig;
import com.platform.model.BroadcastStatus;
import com.platform.model.Contact;
import com.platform.model.LiveBroadcast;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(TestJpaConfig.class)
@ActiveProfiles("test")
class ContactLiveBroadcastRepositoryTest {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private LiveBroadcastRepository liveBroadcastRepository;

    // ==================== ContactRepository ====================

    @Test
    void contactRepository_FindByUserIdOrderByContactNameAsc_ReturnsOrdered() {
        contactRepository.save(Contact.builder().id("c1").userId("u1").contactUserId("u2").contactName("Charlie").build());
        contactRepository.save(Contact.builder().id("c2").userId("u1").contactUserId("u3").contactName("Alice").build());
        contactRepository.save(Contact.builder().id("c3").userId("u1").contactUserId("u4").contactName("Bob").build());

        List<Contact> contacts = contactRepository.findByUserIdOrderByContactNameAsc("u1");
        assertEquals(3, contacts.size());
        assertEquals("Alice", contacts.get(0).getContactName());
        assertEquals("Bob", contacts.get(1).getContactName());
        assertEquals("Charlie", contacts.get(2).getContactName());
    }

    @Test
    void contactRepository_ExistsByUserIdAndContactUserId_Works() {
        contactRepository.save(Contact.builder().id("ce1").userId("u10").contactUserId("u20").contactName("Test").build());

        assertTrue(contactRepository.existsByUserIdAndContactUserId("u10", "u20"));
        assertFalse(contactRepository.existsByUserIdAndContactUserId("u10", "u99"));
    }

    @Test
    void contactRepository_DeleteByUserIdAndContactUserId_RemovesSpecificContact() {
        contactRepository.save(Contact.builder().id("cd1").userId("u30").contactUserId("u40").contactName("Del").build());
        contactRepository.save(Contact.builder().id("cd2").userId("u30").contactUserId("u50").contactName("Keep").build());

        contactRepository.deleteByUserIdAndContactUserId("u30", "u40");
        contactRepository.flush();

        assertFalse(contactRepository.existsByUserIdAndContactUserId("u30", "u40"));
        assertTrue(contactRepository.existsByUserIdAndContactUserId("u30", "u50"));
    }

    // ==================== LiveBroadcastRepository ====================

    @Test
    void liveBroadcastRepository_FindByStatus_ReturnsMatchingBroadcasts() {
        UUID ownerId = UUID.randomUUID();
        liveBroadcastRepository.save(LiveBroadcast.builder().title("Live1").streamType("RTMP").ownerId(ownerId).status(BroadcastStatus.LIVE).version(0L).build());
        liveBroadcastRepository.save(LiveBroadcast.builder().title("Idle1").streamType("RTMP").ownerId(ownerId).status(BroadcastStatus.IDLE).version(0L).build());
        liveBroadcastRepository.save(LiveBroadcast.builder().title("Live2").streamType("WEBRTC").ownerId(ownerId).status(BroadcastStatus.LIVE).version(0L).build());

        List<LiveBroadcast> live = liveBroadcastRepository.findByStatus(BroadcastStatus.LIVE);
        assertEquals(2, live.size());
    }

    @Test
    void liveBroadcastRepository_IncrementAndDecrementViewerCount_Works() {
        LiveBroadcast saved = liveBroadcastRepository.save(LiveBroadcast.builder().title("Viewers").streamType("RTMP")
                .ownerId(UUID.randomUUID()).status(BroadcastStatus.LIVE).viewerCount(10).version(0L).build());
        liveBroadcastRepository.flush();

        liveBroadcastRepository.incrementViewerCount(saved.getId());
        liveBroadcastRepository.flush();
        assertEquals(11, liveBroadcastRepository.findById(saved.getId()).orElseThrow().getViewerCount());

        liveBroadcastRepository.decrementViewerCount(saved.getId());
        liveBroadcastRepository.flush();
        assertEquals(10, liveBroadcastRepository.findById(saved.getId()).orElseThrow().getViewerCount());
    }

    @Test
    void liveBroadcastRepository_FindByRtmpStreamKey_ReturnsBroadcast() {
        UUID ownerId = UUID.randomUUID();
        liveBroadcastRepository.save(LiveBroadcast.builder().title("Stream").streamType("RTMP")
                .ownerId(ownerId).status(BroadcastStatus.LIVE).rtmpStreamKey("key-xyz").version(0L).build());

        Optional<LiveBroadcast> found = liveBroadcastRepository.findByRtmpStreamKey("key-xyz");
        assertTrue(found.isPresent());
        assertEquals("Stream", found.get().getTitle());

        assertFalse(liveBroadcastRepository.findByRtmpStreamKey("nonexistent").isPresent());
    }
}
