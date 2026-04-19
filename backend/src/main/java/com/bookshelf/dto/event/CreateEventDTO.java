package com.bookshelf.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateEventDTO {
    @NotBlank(message = "Название мероприятия обязательно")
    private String title;
    private String description;
    @NotNull(message = "Дата мероприятия обязательна")
    private LocalDate date;
    private String time;
    private String location;
    private Integer maxParticipants;
    private String organizer;
}
