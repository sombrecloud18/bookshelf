package com.bookshelf.controller;

import com.bookshelf.dto.review.CreateReviewDTO;
import com.bookshelf.dto.review.ReviewDTO;
import com.bookshelf.dto.review.UpdateReviewDTO;
import com.bookshelf.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateReviewDTO dto) {
        log.debug("Создание рецензии: userId={}, bookId={}, rating={}", userId, dto.getBookId(), dto.getRating());
        ReviewDTO result = reviewService.createReview(userId, dto);
        log.info("Рецензия создана: id={}, userId={}, bookId={}", result.getId(), userId, dto.getBookId());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewDTO> updateReview(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReviewDTO dto) {
        log.debug("Обновление рецензии: id={}, userId={}", id, userId);
        ReviewDTO result = reviewService.updateReview(userId, id, dto);
        log.info("Рецензия обновлена: id={}", id);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        boolean isModerator = isModerator();
        log.info("Удаление рецензии: id={}, userId={}, isModerator={}", id, userId, isModerator);
        reviewService.deleteReview(userId, id, isModerator);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<Page<ReviewDTO>> getBookReviews(
            @PathVariable UUID bookId,
            @PageableDefault(size = 10) Pageable pageable) {
        log.debug("Рецензии книги: bookId={}, page={}", bookId, pageable.getPageNumber());
        return ResponseEntity.ok(reviewService.getBookReviews(bookId, pageable));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<ReviewDTO>> getMyReviews(
            @AuthenticationPrincipal UUID userId,
            @PageableDefault(size = 10) Pageable pageable) {
        log.debug("Мои рецензии: userId={}, page={}", userId, pageable.getPageNumber());
        return ResponseEntity.ok(reviewService.getUserReviews(userId, pageable));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Page<ReviewDTO>> getPendingReviews(@PageableDefault(size = 20) Pageable pageable) {
        log.debug("Рецензии на модерации: page={}", pageable.getPageNumber());
        return ResponseEntity.ok(reviewService.getPendingReviews(pageable));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<ReviewDTO> approveReview(@PathVariable UUID id) {
        log.info("Одобрение рецензии: id={}", id);
        return ResponseEntity.ok(reviewService.approveReview(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<ReviewDTO> rejectReview(@PathVariable UUID id) {
        log.info("Отклонение рецензии: id={}", id);
        return ResponseEntity.ok(reviewService.rejectReview(id));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeReview(@PathVariable UUID id) {
        log.debug("Лайк рецензии: id={}", id);
        reviewService.likeReview(id);
        return ResponseEntity.ok().build();
    }

    private boolean isModerator() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_MODERATOR"));
    }
}
