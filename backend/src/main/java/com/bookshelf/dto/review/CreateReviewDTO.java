package com.bookshelf.dto.review;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.UUID;

@Data
public class CreateReviewDTO {
    @NotNull(message = "Необходимо выбрать книгу")
    private UUID bookId;

    @NotNull(message = "Оценка обязательна")
    @Min(value = 1, message = "Оценка должна быть от 1 до 5")
    @Max(value = 5, message = "Оценка должна быть от 1 до 5")
    private Integer rating;

    @NotBlank(message = "Текст рецензии обязателен")
    @Size(min = 200, message = "Минимальная длина рецензии — 200 символов")
    private String text;
}
