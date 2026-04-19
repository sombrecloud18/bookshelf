package com.bookshelf.service;

import com.bookshelf.dto.event.CreateEventDTO;
import com.bookshelf.dto.event.EventDTO;
import com.bookshelf.entity.Event;
import com.bookshelf.entity.EventParticipant;
import com.bookshelf.entity.User;
import com.bookshelf.exception.AppException;
import com.bookshelf.repository.EventParticipantRepository;
import com.bookshelf.repository.EventRepository;
import com.bookshelf.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final UserRepository userRepository;

    public Page<EventDTO> getUpcomingEvents(Pageable pageable) {
        return eventRepository.findUpcoming(pageable).map(e -> toDTO(e, 0));
    }

    public EventDTO getEventById(UUID id) {
        Event event = findById(id);
        long count = eventParticipantRepository.countByEventId(id);
        return toDTO(event, count);
    }

    @Transactional
    public EventDTO createEvent(UUID userId, CreateEventDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("Пользователь не найден"));

        Event event = Event.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .date(dto.getDate())
                .time(dto.getTime())
                .location(dto.getLocation())
                .maxParticipants(dto.getMaxParticipants())
                .organizer(dto.getOrganizer())
                .createdBy(user)
                .build();

        event = eventRepository.save(event);
        long count = eventParticipantRepository.countByEventId(event.getId());
        return toDTO(event, count);
    }

    @Transactional
    public EventDTO updateEvent(UUID id, CreateEventDTO dto) {
        Event event = findById(id);

        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getDate() != null) event.setDate(dto.getDate());
        if (dto.getTime() != null) event.setTime(dto.getTime());
        if (dto.getLocation() != null) event.setLocation(dto.getLocation());
        if (dto.getMaxParticipants() != null) event.setMaxParticipants(dto.getMaxParticipants());
        if (dto.getOrganizer() != null) event.setOrganizer(dto.getOrganizer());

        event = eventRepository.save(event);
        long count = eventParticipantRepository.countByEventId(event.getId());
        return toDTO(event, count);
    }

    @Transactional
    public void deleteEvent(UUID id) {
        if (!eventRepository.existsById(id)) {
            throw AppException.notFound("Мероприятие не найдено");
        }
        eventRepository.deleteById(id);
    }

    @Transactional
    public void registerForEvent(UUID userId, UUID eventId) {
        Event event = findById(eventId);

        if (eventParticipantRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw AppException.conflict("Вы уже зарегистрированы на это мероприятие");
        }

        long count = eventParticipantRepository.countByEventId(eventId);
        if (event.getMaxParticipants() != null && count >= event.getMaxParticipants()) {
            throw AppException.conflict("Достигнуто максимальное количество участников");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("Пользователь не найден"));

        EventParticipant participant = EventParticipant.builder()
                .id(new EventParticipant.EventParticipantId(eventId, userId))
                .event(event)
                .user(user)
                .build();

        eventParticipantRepository.save(participant);
    }

    @Transactional
    public void unregisterFromEvent(UUID userId, UUID eventId) {
        if (!eventParticipantRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw AppException.notFound("Регистрация не найдена");
        }
        eventParticipantRepository.deleteByEventIdAndUserId(eventId, userId);
    }

    public boolean isRegistered(UUID userId, UUID eventId) {
        return eventParticipantRepository.existsByEventIdAndUserId(eventId, userId);
    }

    public Event findById(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Мероприятие не найдено"));
    }

    public EventDTO toDTO(Event e, long currentParticipants) {
        return EventDTO.builder()
                .id(e.getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .date(e.getDate())
                .time(e.getTime())
                .location(e.getLocation())
                .maxParticipants(e.getMaxParticipants())
                .currentParticipants(currentParticipants)
                .organizer(e.getOrganizer())
                .build();
    }
}
