package com.bookshelf.dto.review;

import com.bookshelf.dto.comment.CommentDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewDTO {
    private UUID id;
    private UUID bookId;
    private String bookTitle;
    private String author;
    private String genre;
    private String coverUrl;
    private UUID userId;
    private String userName;
    private String userAvatar;
    private Integer rating;
    private String text;
    private String status;
    private Integer likes;
    private OffsetDateTime createdAt;
    private List<CommentDTO> comments;
    // For pending reviews (moderator view)
    private String reviewAuthor;
    private String reviewAuthorName;
}
