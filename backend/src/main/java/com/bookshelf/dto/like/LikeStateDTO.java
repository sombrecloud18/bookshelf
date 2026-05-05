package com.bookshelf.dto.like;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Snapshot of likes for a target — used both as REST response and as STOMP broadcast payload. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LikeStateDTO {
    private String targetType; // REVIEW | COMMENT
    private UUID targetId;
    private long count;
    /** Whether the requesting user has liked. Null when broadcast (recipient-specific). */
    private Boolean liked;
}
