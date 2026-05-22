package com.platform.integration;

import com.platform.PlatformApplication;
import com.platform.model.BroadcastStatus;
import com.platform.model.LiveBroadcast;
import com.platform.repository.LiveBroadcastRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LiveBroadcastIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private LiveBroadcastRepository broadcastRepository;

    @BeforeEach
    void setUp() {
        broadcastRepository.deleteAll();
    }

    @Test
    void testGetActiveBroadcastsEmpty() {
        ResponseEntity<LiveBroadcast[]> response = restTemplate
            .getForEntity("http://localhost:" + port + "/api/broadcast/active",
                LiveBroadcast[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().length);
    }

    @Test
    void testGetBroadcastNotFound() {
        ResponseEntity<LiveBroadcast> response = restTemplate
            .getForEntity("http://localhost:" + port + "/api/broadcast/nonexistent",
                LiveBroadcast.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testRepositoryOperations() {
        LiveBroadcast broadcast = LiveBroadcast.builder()
            .id("test-id-1")
            .title("Test Broadcast")
            .description("Test Description")
            .streamType("video")
            .ownerId("test-user")
            .status(BroadcastStatus.IDLE)
            .rtmpStreamKey("test-key")
            .build();

        LiveBroadcast saved = broadcastRepository.save(broadcast);
        assertNotNull(saved);
        assertEquals("Test Broadcast", saved.getTitle());

        var activeBroadcasts = broadcastRepository.findByStatus(BroadcastStatus.IDLE);
        assertFalse(activeBroadcasts.isEmpty());

        broadcastRepository.deleteAll();
    }
}
