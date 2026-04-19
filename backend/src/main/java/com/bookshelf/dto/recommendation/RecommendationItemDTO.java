package com.bookshelf.dto.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RecommendationItemDTO {
    private UUID id;
    private UUID bookId;
    private String title;
    private String genre;
    private String author;
    private String imageUrl;
    private String description;
    private Double matchScore;
    private Long views;
    private String addedDate;
}
