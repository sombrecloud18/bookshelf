package com.bookshelf.dto.book;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBookDTO {
    @NotBlank(message = "Название книги обязательно")
    private String title;

    @NotBlank(message = "Автор книги обязателен")
    private String author;

    private String genre;
    private Integer year;
    private String description;
    private String fullDescription;
    private String imageUrl;
    private String coverUrl;
    private Integer pages;
    private String publisher;
    private Integer publishYear;
    private String isbn;
}
