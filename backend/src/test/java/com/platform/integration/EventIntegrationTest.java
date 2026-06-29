package com.platform.integration;

import com.platform.model.Event;
import com.platform.model.dto.EventDto;
import com.platform.repository.EventRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Event CRUD flow.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EventIntegrationTest extends AbstractIntegrationTest {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Autowired private EventRepository eventRepository;

    private String adminUserId;
    private org.springframework.security.oauth2.jwt.Jwt adminJwt;
    private org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken adminAuth;

    @BeforeEach
    void setUp() {
        adminUserId = userId();
        adminJwt = createJwt(adminUserId, "admin", "SUPER_ADMIN");
        adminAuth = createAuth(adminJwt, "SUPER_ADMIN");
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/events — create event as admin")
    void createEvent() throws Exception {
        var request = new EventDto.CreateRequest();
        request.setTitle("Test Integration Event");
        request.setDescription("Event description for testing");
        request.setStartTime(LocalDateTime.now().plusDays(7));
        request.setEndTime(LocalDateTime.now().plusDays(7).plusHours(2));
        // Skip links — H2 JSON type can't deserialize Map<String,String> on read

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/events")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Integration Event"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/events/upcoming — list upcoming events")
    void getUpcomingEvents() throws Exception {
        eventRepository.save(Event.builder()
                .title("Upcoming Event")
                .description("Test")
                .startTime(LocalDateTime.now().plusDays(1))
                .createdBy(adminUserId)
                .status("scheduled")
                .build());

        mockMvc.perform(get("/api/events/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/events/next — get next upcoming event")
    void getNextEvent() throws Exception {
        eventRepository.save(Event.builder()
                .title("Next Event")
                .description("Next one")
                .startTime(LocalDateTime.now().plusHours(2))
                .createdBy(adminUserId)
                .status("scheduled")
                .build());

        mockMvc.perform(get("/api/events/next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Next Event"));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/events/{id} — get event by ID")
    void getEventById() throws Exception {
        Event saved = eventRepository.save(Event.builder()
                .title("Specific Event")
                .description("Details here")
                .startTime(LocalDateTime.now().plusDays(3))
                .createdBy(adminUserId)
                .build());

        mockMvc.perform(get("/api/events/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Specific Event"));
    }

    @Test
    @Order(5)
    @DisplayName("PUT /api/events/{id} — update event")
    void updateEvent() throws Exception {
        Event saved = eventRepository.save(Event.builder()
                .title("Old Title")
                .description("Old description")
                .startTime(LocalDateTime.now().plusDays(5))
                .createdBy(adminUserId)
                .build());

        var updateRequest = new EventDto.UpdateRequest();
        updateRequest.setTitle("New Title");
        updateRequest.setStatus("cancelled");

        mockMvc.perform(put("/api/events/{id}", saved.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.status").value("cancelled"));
    }

    @Test
    @Order(6)
    @DisplayName("DELETE /api/events/{id} — delete event")
    void deleteEvent() throws Exception {
        Event saved = eventRepository.save(Event.builder()
                .title("Event to Delete")
                .startTime(LocalDateTime.now().plusDays(10))
                .createdBy(adminUserId)
                .build());

        mockMvc.perform(delete("/api/events/{id}", saved.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/events — 403 for non-admin")
    void createEventForbiddenForUser() throws Exception {
        var userJwt = createJwt(userId(), "user", "USER_0");
        var userAuth = createAuth(userJwt, "USER_0");

        var request = new EventDto.CreateRequest();
        request.setTitle("Should Fail");
        request.setStartTime(LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/events")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(userAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/events/past — past events")
    void getPastEvents() throws Exception {
        eventRepository.save(Event.builder()
                .title("Past Event")
                .startTime(LocalDateTime.now().minusDays(5))
                .createdBy(adminUserId)
                .status("completed")
                .build());

        mockMvc.perform(get("/api/events/past"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
