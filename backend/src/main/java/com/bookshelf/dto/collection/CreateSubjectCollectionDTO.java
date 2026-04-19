package com.bookshelf.dto.collection;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class CreateSubjectCollectionDTO {
    @NotBlank(message = "Предмет обязателен")
    private String subject;
    @NotBlank(message = "Специальность обязательна")
    private String specialty;
    @NotBlank(message = "Название специальности обязательно")
    private String specialtyName;
    @NotBlank(message = "Название подборки обязательно")
    private String title;
    private String description;
    private List<UUID> bookIds;
    private String authorRole;
}
