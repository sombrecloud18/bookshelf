package com.bookshelf.controller;

import com.bookshelf.dto.event.CreateEventDTO;
import com.bookshelf.dto.event.EventDTO;
import com.bookshelf.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<Page<EventDTO>> getUpcomingEvents(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(eventService.getUpcomingEvents(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<EventDTO> createEvent(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateEventDTO dto) {
        return ResponseEntity.ok(eventService.createEvent(userId, dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<EventDTO> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody CreateEventDTO dto) {
        return ResponseEntity.ok(eventService.updateEvent(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<Void> registerForEvent(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        eventService.registerForEvent(userId, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/register")
    public ResponseEntity<Void> unregisterFromEvent(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        eventService.unregisterFromEvent(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/registered")
    public ResponseEntity<Map<String, Boolean>> checkRegistration(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        boolean registered = userId != null && eventService.isRegistered(userId, id);
        return ResponseEntity.ok(Map.of("registered", registered));
    }
}
