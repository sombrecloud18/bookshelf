package com.bookshelf.dto.subject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSubjectDTO {
    @NotBlank(message = "Специальность обязательна")
    @Size(max = 50)
    private String specialty;

    @NotBlank(message = "Название предмета обязательно")
    @Size(max = 255)
    private String name;
}
