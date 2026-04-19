package com.bookshelf.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class CreateOrderDTO {
    @NotNull(message = "ID книги обязателен")
    private UUID bookId;
}
