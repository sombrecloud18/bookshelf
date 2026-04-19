package com.bookshelf.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CollectionDTO {
    private UUID id;
    private UUID userId;
    private String title;
    private String genre;
    private String description;
    private String status;
    private List<UUID> bookIds;
    private String author;
    private String authorName;
    private OffsetDateTime createdAt;
}
