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
    private Double matchScore;     // 0..1, where 1 = strongest recommendation
    private String source;         // CF / CB / HYBRID / POPULAR / NEW / SIMILAR_CF / SIMILAR_GENRE
    private Long views;
    private String addedDate;
}
