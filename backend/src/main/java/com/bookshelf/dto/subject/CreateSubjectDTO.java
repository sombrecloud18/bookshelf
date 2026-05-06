package com.bookshelf.dto.subject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateSubjectDTO {
    @NotBlank(message = "Название предмета обязательно")
    @Size(max = 255)
    private String name;

    /** When true the subject is shared across every specialty (specialtyIds is ignored). */
    private boolean common;

    /** Explicit list of specialties to link this subject to. Required when {@code common} is false. */
    private List<UUID> specialtyIds;
}
