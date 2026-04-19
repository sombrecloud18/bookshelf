package com.bookshelf.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubjectCollectionDTO {
    private UUID id;
    private String subject;
    private String specialty;
    private String specialtyName;
    private String title;
    private String description;
    private List<UUID> bookIds;
    private String author;
    private UUID authorId;
    private String authorRole;
    private String status;
    private OffsetDateTime createdAt;
    private String moderatorComment;
}
