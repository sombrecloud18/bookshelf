package com.bookshelf.dto.study;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFacultyDTO {
    @NotBlank
    @Size(min = 1, max = 100)
    private String name;
}
