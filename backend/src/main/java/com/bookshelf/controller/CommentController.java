package com.bookshelf.controller;

import com.bookshelf.dto.comment.CommentDTO;
import com.bookshelf.dto.comment.CreateCommentDTO;
import com.bookshelf.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/review/{reviewId}")
    public ResponseEntity<CommentDTO> addToReview(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID reviewId,
            @Valid @RequestBody CreateCommentDTO dto) {
        log.debug("Комментарий к рецензии: userId={}, reviewId={}", userId, reviewId);
        CommentDTO result = commentService.addCommentToReview(userId, reviewId, dto);
        log.info("Комментарий добавлен к рецензии: id={}, reviewId={}", result.getId(), reviewId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/collection/{collectionId}")
    public ResponseEntity<CommentDTO> addToCollection(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID collectionId,
            @Valid @RequestBody CreateCommentDTO dto) {
        log.debug("Комментарий к подборке: userId={}, collectionId={}", userId, collectionId);
        CommentDTO result = commentService.addCommentToCollection(userId, collectionId, dto);
        log.info("Комментарий добавлен к подборке: id={}, collectionId={}", result.getId(), collectionId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentDTO> updateComment(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateCommentDTO dto) {
        log.debug("Обновление комментария: id={}, userId={}", id, userId);
        return ResponseEntity.ok(commentService.updateComment(userId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        boolean isModerator = isModerator();
        log.info("Удаление комментария: id={}, userId={}, isModerator={}", id, userId, isModerator);
        commentService.deleteComment(userId, id, isModerator);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/review/{reviewId}")
    public ResponseEntity<List<CommentDTO>> getReviewComments(@PathVariable UUID reviewId) {
        log.debug("Комментарии рецензии: reviewId={}", reviewId);
        return ResponseEntity.ok(commentService.getCommentsForReview(reviewId));
    }

    @GetMapping("/collection/{collectionId}")
    public ResponseEntity<List<CommentDTO>> getCollectionComments(@PathVariable UUID collectionId) {
        log.debug("Комментарии подборки: collectionId={}", collectionId);
        return ResponseEntity.ok(commentService.getCommentsForCollection(collectionId));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeComment(@PathVariable UUID id) {
        log.debug("Лайк комментария: id={}", id);
        commentService.likeComment(id);
        return ResponseEntity.ok().build();
    }

    private boolean isModerator() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_MODERATOR"));
    }
}
