package com.bookshelf.dto.study;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateSpecialtyDTO {
    @NotNull
    private UUID facultyId;

    @NotBlank
    @Size(min = 1, max = 255)
    private String name;
}
