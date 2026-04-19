package com.bookshelf.dto.review;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateReviewDTO {
    @NotNull(message = "Оценка обязательна")
    @Min(value = 1, message = "Оценка должна быть от 1 до 5")
    @Max(value = 5, message = "Оценка должна быть от 1 до 5")
    private Integer rating;

    @NotBlank(message = "Текст рецензии обязателен")
    @Size(min = 200, message = "Минимальная длина рецензии — 200 символов")
    private String text;
}
