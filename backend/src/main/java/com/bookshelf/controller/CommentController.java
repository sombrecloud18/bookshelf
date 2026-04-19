package com.bookshelf.controller;

import com.bookshelf.dto.comment.CommentDTO;
import com.bookshelf.dto.comment.CreateCommentDTO;
import com.bookshelf.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/review/{reviewId}")
    public ResponseEntity<CommentDTO> addToReview(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID reviewId,
            @Valid @RequestBody CreateCommentDTO dto) {
        return ResponseEntity.ok(commentService.addCommentToReview(userId, reviewId, dto));
    }

    @PostMapping("/collection/{collectionId}")
    public ResponseEntity<CommentDTO> addToCollection(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID collectionId,
            @Valid @RequestBody CreateCommentDTO dto) {
        return ResponseEntity.ok(commentService.addCommentToCollection(userId, collectionId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentDTO> updateComment(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateCommentDTO dto) {
        return ResponseEntity.ok(commentService.updateComment(userId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        boolean isModerator = isModerator();
        commentService.deleteComment(userId, id, isModerator);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/review/{reviewId}")
    public ResponseEntity<List<CommentDTO>> getReviewComments(@PathVariable UUID reviewId) {
        return ResponseEntity.ok(commentService.getCommentsForReview(reviewId));
    }

    @GetMapping("/collection/{collectionId}")
    public ResponseEntity<List<CommentDTO>> getCollectionComments(@PathVariable UUID collectionId) {
        return ResponseEntity.ok(commentService.getCommentsForCollection(collectionId));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeComment(@PathVariable UUID id) {
        commentService.likeComment(id);
        return ResponseEntity.ok().build();
    }

    private boolean isModerator() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_MODERATOR"));
    }
}
