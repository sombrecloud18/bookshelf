package com.bookshelf.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Lightweight payload broadcast over STOMP whenever a registration changes.
 * Clients use it to keep the participant counter live without polling
 * or having to reopen the event details modal.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EventStateDTO {
    private UUID eventId;
    private long currentParticipants;
    private Integer maxParticipants;
}
