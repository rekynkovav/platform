import com.platform.util.RoleUtils;
// src/main/java/com/platform/service/EventService.java
package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Event;
import com.platform.model.dto.EventDto;
import com.platform.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;

    public List<EventDto> getUpcomingEvents() {
        List<Event> events = eventRepository.findUpcoming(LocalDateTime.now());
        return events.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<EventDto> getPastEvents() {
        List<Event> events = eventRepository.findPast(LocalDateTime.now());
        return events.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public EventDto getNextEvent() {
        return eventRepository.findNextEvent(LocalDateTime.now())
                .map(this::toDto)
                .orElse(null);
    }

    public EventDto getEvent(String id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
        return toDto(event);
    }

    public long getTotalCount() {
        return eventRepository.count();
    }

    @Transactional
    public EventDto createEvent(EventDto.CreateRequest request, org.springframework.security.oauth2.jwt.Jwt jwt) {
        String userId = jwt.getSubject();

        Event event = Event.builder()
                .id(UUID.randomUUID().toString())
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .coverImage(request.getCoverImage())
                .links(request.getLinks())
                .streamUrl(request.getStreamUrl())
                .status("scheduled")
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        event = eventRepository.save(event);
        log.info("Event created: {} by {}", event.getId(), userId);

        return toDto(event);
    }

    @Transactional
    public EventDto updateEvent(String id, EventDto.UpdateRequest request, org.springframework.security.oauth2.jwt.Jwt jwt) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));

        String userId = jwt.getSubject();
        String userRole = getUserRole(jwt);

        if (!event.getCreatedBy().equals(userId) && !isAdmin(userRole)) {
            throw new AccessDeniedException("You don't have permission to update this event");
        }

        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getStartTime() != null) event.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) event.setEndTime(request.getEndTime());
        if (request.getCoverImage() != null) event.setCoverImage(request.getCoverImage());
        if (request.getLinks() != null) event.setLinks(request.getLinks());
        if (request.getStreamUrl() != null) event.setStreamUrl(request.getStreamUrl());
        if (request.getStatus() != null) event.setStatus(request.getStatus());

        event = eventRepository.save(event);
        log.info("Event updated: {} by {}", id, userId);

        return toDto(event);
    }

    @Transactional
    public void deleteEvent(String id, org.springframework.security.oauth2.jwt.Jwt jwt) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));

        String userId = jwt.getSubject();
        String userRole = getUserRole(jwt);

        if (!event.getCreatedBy().equals(userId) && !isAdmin(userRole)) {
            throw new AccessDeniedException("You don't have permission to delete this event");
        }

        eventRepository.delete(event);
        log.info("Event deleted: {} by {}", id, userId);
    }

    private EventDto toDto(Event event) {
        return EventDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .coverImage(event.getCoverImage())
                .links(event.getLinks())
                .streamUrl(event.getStreamUrl())
                .status(event.getStatus())
                .createdBy(event.getCreatedBy())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private String getUserRole(org.springframework.security.oauth2.jwt.Jwt jwt) {
        return RoleUtils.getUserRole(jwt);
    }

    private boolean isAdmin(String role) {
        return "MODERATOR_ADMIN".equals(role) || "SUPER_ADMIN".equals(role) || "VYACHESLAV".equals(role);
    }
}