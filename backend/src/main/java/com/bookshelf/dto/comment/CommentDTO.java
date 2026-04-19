package com.bookshelf.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CommentDTO {
    private UUID id;
    private UUID userId;
    private String userName;
    private String userAvatar;
    private String text;
    private Integer likes;
    private OffsetDateTime createdAt;
}
