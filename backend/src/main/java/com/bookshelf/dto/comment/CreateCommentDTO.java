package com.bookshelf.dto.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCommentDTO {
    @NotBlank(message = "Текст комментария обязателен")
    private String text;
}
