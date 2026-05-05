package com.bookshelf.controller;

import com.bookshelf.dto.event.CreateEventDTO;
import com.bookshelf.dto.event.EventDTO;
import com.bookshelf.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<Page<EventDTO>> getUpcomingEvents(@PageableDefault(size = 10) Pageable pageable) {
        log.debug("Предстоящие мероприятия: page={}", pageable.getPageNumber());
        return ResponseEntity.ok(eventService.getUpcomingEvents(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getEvent(@PathVariable UUID id) {
        log.debug("Получение мероприятия: id={}", id);
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<EventDTO> createEvent(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateEventDTO dto) {
        log.info("Создание мероприятия: title='{}', userId={}", dto.getTitle(), userId);
        EventDTO result = eventService.createEvent(userId, dto);
        log.info("Мероприятие создано: id={}, title='{}'", result.getId(), result.getTitle());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<EventDTO> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody CreateEventDTO dto) {
        log.info("Обновление мероприятия: id={}", id);
        EventDTO result = eventService.updateEvent(id, dto);
        log.info("Мероприятие обновлено: id={}", id);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
        log.info("Удаление мероприятия: id={}", id);
        eventService.deleteEvent(id);
        log.info("Мероприятие удалено: id={}", id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<Void> registerForEvent(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        log.info("Регистрация на мероприятие: userId={}, eventId={}", userId, id);
        eventService.registerForEvent(userId, id);
        log.info("Регистрация выполнена: userId={}, eventId={}", userId, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/register")
    public ResponseEntity<Void> unregisterFromEvent(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        log.info("Отмена регистрации: userId={}, eventId={}", userId, id);
        eventService.unregisterFromEvent(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/registered")
    public ResponseEntity<Map<String, Boolean>> checkRegistration(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        log.debug("Проверка регистрации: userId={}, eventId={}", userId, id);
        boolean registered = userId != null && eventService.isRegistered(userId, id);
        return ResponseEntity.ok(Map.of("registered", registered));
    }
}
