package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Event;
import com.platform.model.dto.EventDto;
import com.platform.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Unit Tests")
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    private Event testEvent;
    private Jwt creatorJwt;
    private Jwt adminJwt;
    private Jwt otherJwt;

    @BeforeEach
    void setUp() {
        testEvent = Event.builder()
                .id("event-1")
                .title("Test Event")
                .description("Description")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .coverImage("https://example.com/cover.jpg")
                .links(Map.of("telegram", "https://t.me/test"))
                .streamUrl("rtmp://stream.example.com/live")
                .status("scheduled")
                .createdBy("user-1")
                .createdAt(LocalDateTime.now())
                .build();

        creatorJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-1")
                .claim("preferred_username", "creator")
                .claim("realm_access", Map.of("roles", List.of("USER")))
                .build();

        adminJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("admin-1")
                .claim("preferred_username", "admin")
                .claim("realm_access", Map.of("roles", List.of("SUPER_ADMIN")))
                .build();

        otherJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-2")
                .claim("preferred_username", "other")
                .claim("realm_access", Map.of("roles", List.of("USER")))
                .build();
    }

    @Test
    @DisplayName("getUpcomingEvents - returns upcoming events")
    void getUpcomingEvents_ShouldReturnList() {
        when(eventRepository.findUpcoming(any(LocalDateTime.class))).thenReturn(List.of(testEvent));

        List<EventDto> result = eventService.getUpcomingEvents();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Event");
    }

    @Test
    @DisplayName("getPastEvents - returns past events")
    void getPastEvents_ShouldReturnList() {
        when(eventRepository.findPast(any(LocalDateTime.class), any(Pageable.class))).thenReturn(List.of(testEvent));

        List<EventDto> result = eventService.getPastEvents(50);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getNextEvent - returns next event when exists")
    void getNextEvent_WhenExists_ShouldReturn() {
        when(eventRepository.findNextEvent(any(LocalDateTime.class))).thenReturn(Optional.of(testEvent));

        EventDto result = eventService.getNextEvent();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("event-1");
    }

    @Test
    @DisplayName("getNextEvent - returns null when no upcoming events")
    void getNextEvent_WhenNone_ShouldReturnNull() {
        when(eventRepository.findNextEvent(any(LocalDateTime.class))).thenReturn(Optional.empty());

        EventDto result = eventService.getNextEvent();

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getEvent - returns event by id")
    void getEvent_WhenExists_ShouldReturn() {
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(testEvent));

        EventDto result = eventService.getEvent("event-1");

        assertThat(result.getTitle()).isEqualTo("Test Event");
        assertThat(result.getStatus()).isEqualTo("scheduled");
    }

    @Test
    @DisplayName("getEvent - throws when not found")
    void getEvent_WhenNotFound_ShouldThrow() {
        when(eventRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEvent("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Event not found");
    }

    @Test
    @DisplayName("getTotalCount - returns count")
    void getTotalCount_ShouldReturnCount() {
        when(eventRepository.count()).thenReturn(15L);

        assertThat(eventService.getTotalCount()).isEqualTo(15L);
    }

    @Test
    @DisplayName("createEvent - should create and return event")
    void createEvent_ShouldCreate() {
        EventDto.CreateRequest request = EventDto.CreateRequest.builder()
                .title("New Event")
                .description("New Description")
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(3))
                .coverImage("https://example.com/new.jpg")
                .links(Map.of("vk", "https://vk.com/test"))
                .streamUrl("rtmp://new.stream")
                .build();

        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId("new-event-id");
            return e;
        });

        EventDto result = eventService.createEvent(request, creatorJwt);

        assertThat(result.getTitle()).isEqualTo("New Event");
        assertThat(result.getStatus()).isEqualTo("scheduled");
        assertThat(result.getCreatedBy()).isEqualTo("user-1");
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("updateEvent - creator can update")
    void updateEvent_ByCreator_ShouldUpdate() {
        EventDto.UpdateRequest request = EventDto.UpdateRequest.builder()
                .title("Updated Title")
                .status("live")
                .build();

        when(eventRepository.findById("event-1")).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventDto result = eventService.updateEvent("event-1", request, creatorJwt);

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getStatus()).isEqualTo("live");
        assertThat(result.getDescription()).isEqualTo("Description");
    }

    @Test
    @DisplayName("updateEvent - admin can update any event")
    void updateEvent_ByAdmin_ShouldUpdate() {
        EventDto.UpdateRequest request = EventDto.UpdateRequest.builder()
                .title("Admin Updated")
                .build();

        when(eventRepository.findById("event-1")).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventDto result = eventService.updateEvent("event-1", request, adminJwt);

        assertThat(result.getTitle()).isEqualTo("Admin Updated");
    }

    @Test
    @DisplayName("updateEvent - non-creator non-admin throws AccessDenied")
    void updateEvent_ByOther_ShouldThrow() {
        EventDto.UpdateRequest request = EventDto.UpdateRequest.builder()
                .title("Hacked")
                .build();

        when(eventRepository.findById("event-1")).thenReturn(Optional.of(testEvent));

        assertThatThrownBy(() -> eventService.updateEvent("event-1", request, otherJwt))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("updateEvent - not found throws ResourceNotFoundException")
    void updateEvent_NotFound_ShouldThrow() {
        EventDto.UpdateRequest request = EventDto.UpdateRequest.builder().title("x").build();
        when(eventRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.updateEvent("nonexistent", request, creatorJwt))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateEvent - partial update only changes provided fields")
    void updateEvent_PartialUpdate_ShouldOnlyChangeProvided() {
        EventDto.UpdateRequest request = EventDto.UpdateRequest.builder()
                .description("Only description changed")
                .build();

        when(eventRepository.findById("event-1")).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventDto result = eventService.updateEvent("event-1", request, creatorJwt);

        assertThat(result.getDescription()).isEqualTo("Only description changed");
        assertThat(result.getTitle()).isEqualTo("Test Event");
        assertThat(result.getStatus()).isEqualTo("scheduled");
    }

    @Test
    @DisplayName("deleteEvent - creator can delete")
    void deleteEvent_ByCreator_ShouldDelete() {
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(testEvent));

        eventService.deleteEvent("event-1", creatorJwt);

        verify(eventRepository).delete(testEvent);
    }

    @Test
    @DisplayName("deleteEvent - admin can delete")
    void deleteEvent_ByAdmin_ShouldDelete() {
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(testEvent));

        eventService.deleteEvent("event-1", adminJwt);

        verify(eventRepository).delete(testEvent);
    }

    @Test
    @DisplayName("deleteEvent - non-creator non-admin throws AccessDenied")
    void deleteEvent_ByOther_ShouldThrow() {
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(testEvent));

        assertThatThrownBy(() -> eventService.deleteEvent("event-1", otherJwt))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("deleteEvent - not found throws ResourceNotFoundException")
    void deleteEvent_NotFound_ShouldThrow() {
        when(eventRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.deleteEvent("nonexistent", creatorJwt))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
