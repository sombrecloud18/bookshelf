package com.bookshelf.dto.book;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BookDTO {
    private UUID id;
    private String title;
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
    private Double averageRating;
}
