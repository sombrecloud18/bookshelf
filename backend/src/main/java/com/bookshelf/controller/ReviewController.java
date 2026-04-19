package com.bookshelf.controller;

import com.bookshelf.dto.review.CreateReviewDTO;
import com.bookshelf.dto.review.ReviewDTO;
import com.bookshelf.dto.review.UpdateReviewDTO;
import com.bookshelf.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateReviewDTO dto) {
        return ResponseEntity.ok(reviewService.createReview(userId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewDTO> updateReview(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReviewDTO dto) {
        return ResponseEntity.ok(reviewService.updateReview(userId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        boolean isModerator = isModerator();
        reviewService.deleteReview(userId, id, isModerator);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<Page<ReviewDTO>> getBookReviews(
            @PathVariable UUID bookId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getBookReviews(bookId, pageable));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<ReviewDTO>> getMyReviews(
            @AuthenticationPrincipal UUID userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getUserReviews(userId, pageable));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Page<ReviewDTO>> getPendingReviews(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getPendingReviews(pageable));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<ReviewDTO> approveReview(@PathVariable UUID id) {
        return ResponseEntity.ok(reviewService.approveReview(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<ReviewDTO> rejectReview(@PathVariable UUID id) {
        return ResponseEntity.ok(reviewService.rejectReview(id));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeReview(@PathVariable UUID id) {
        reviewService.likeReview(id);
        return ResponseEntity.ok().build();
    }

    private boolean isModerator() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_MODERATOR"));
    }
}
