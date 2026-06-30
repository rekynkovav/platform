package com.platform.controller;

import com.platform.model.dto.EventDto;
import com.platform.service.EventService;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping("/upcoming")
    public ResponseEntity<List<EventDto>> getUpcomingEvents() {
        return ResponseEntity.ok(eventService.getUpcomingEvents());
    }

    @GetMapping("/past")
    public ResponseEntity<List<EventDto>> getPastEvents(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(eventService.getPastEvents(limit));
    }

    @GetMapping("/next")
    public ResponseEntity<EventDto> getNextEvent() {
        EventDto event = eventService.getNextEvent();
        return event != null ? ResponseEntity.ok(event) : ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDto> getEvent(@PathVariable String id) {
        return ResponseEntity.ok(eventService.getEvent(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    @RateLimiting(name = "event-limit")
    public ResponseEntity<EventDto> createEvent(
            @Valid @RequestBody EventDto.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(eventService.createEvent(request, jwt));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    @RateLimiting(name = "event-limit")
    public ResponseEntity<EventDto> updateEvent(
            @PathVariable String id,
            @Valid @RequestBody EventDto.UpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(eventService.updateEvent(id, request, jwt));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    @RateLimiting(name = "event-limit")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        eventService.deleteEvent(id, jwt);
        return ResponseEntity.noContent().build();
    }
}
