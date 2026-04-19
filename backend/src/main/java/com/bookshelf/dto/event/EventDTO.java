package com.bookshelf.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EventDTO {
    private UUID id;
    private String title;
    private String description;
    private LocalDate date;
    private String time;
    private String location;
    private Integer maxParticipants;
    private long currentParticipants;
    private String organizer;
}
