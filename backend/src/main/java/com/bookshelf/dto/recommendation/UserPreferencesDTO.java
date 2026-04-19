package com.bookshelf.dto.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserPreferencesDTO {
    private List<String> favoriteGenres;
    private List<String> favoriteAuthors;
    private List<String> readingHistory;
}
