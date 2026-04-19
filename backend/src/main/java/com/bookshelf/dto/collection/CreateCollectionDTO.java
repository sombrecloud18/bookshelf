package com.bookshelf.dto.collection;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class CreateCollectionDTO {
    @NotBlank(message = "Название подборки обязательно")
    private String title;
    private String genre;
    private String description;
    private List<UUID> bookIds;
}
