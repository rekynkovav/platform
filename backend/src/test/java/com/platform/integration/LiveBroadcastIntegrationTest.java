package com.platform.integration;

import com.platform.PlatformApplication;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class LiveBroadcastIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

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
    void testCreateAndGetBroadcast() {
        // Create broadcast
        Map<String, String> request = Map.of(
            "title", "Integration Test Broadcast",
            "description", "Test Description",
            "streamType", "video"
        );

        ResponseEntity<LiveBroadcast> createResponse = restTemplate
            .withBasicAuth("user", "password")
            .postForEntity("http://localhost:" + port + "/api/broadcast/create",
                request, LiveBroadcast.class);

        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        String broadcastId = createResponse.getBody().getId();

        // Get broadcast
        ResponseEntity<LiveBroadcast> getResponse = restTemplate
            .getForEntity("http://localhost:" + port + "/api/broadcast/" + broadcastId,
                LiveBroadcast.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("Integration Test Broadcast", getResponse.getBody().getTitle());
    }

    @Test
    void testGetActiveBroadcastsEmpty() {
        ResponseEntity<LiveBroadcast[]> response = restTemplate
            .getForEntity("http://localhost:" + port + "/api/broadcast/active",
                LiveBroadcast[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().length);
    }
}