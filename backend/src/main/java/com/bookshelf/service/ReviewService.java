package com.bookshelf.service;

import com.bookshelf.dto.comment.CommentDTO;
import com.bookshelf.dto.review.CreateReviewDTO;
import com.bookshelf.dto.review.ReviewDTO;
import com.bookshelf.dto.review.UpdateReviewDTO;
import com.bookshelf.entity.*;
import com.bookshelf.exception.AppException;
import com.bookshelf.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final UserActivityRepository userActivityRepository;
    private final LikeService likeService;

    @Transactional
    public ReviewDTO createReview(UUID userId, CreateReviewDTO dto) {
        if (reviewRepository.existsByBookIdAndUserId(dto.getBookId(), userId)) {
            throw AppException.conflict("Вы уже написали рецензию на эту книгу");
        }

        Book book = bookRepository.findById(dto.getBookId())
                .orElseThrow(() -> AppException.notFound("Книга не найдена"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("Пользователь не найден"));

        Review review = Review.builder()
                .book(book)
                .user(user)
                .rating(dto.getRating())
                .text(dto.getText())
                .status("PENDING")
                .build();

        review = reviewRepository.save(review);
        log.info("Рецензия сохранена (ожидает модерации): id={}, bookId={}, userId={}",
                review.getId(), dto.getBookId(), userId);

        userActivityRepository.save(UserActivity.builder()
                .user(user).book(book).activityType("REVIEW").build());

        return toDTO(review, userId);
    }

    @Transactional
    public ReviewDTO updateReview(UUID userId, UUID reviewId, UpdateReviewDTO dto) {
        Review review = findById(reviewId);

        if (!review.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Вы не можете редактировать чужую рецензию");
        }

        review.setRating(dto.getRating());
        review.setText(dto.getText());
        review.setStatus("PENDING");
        return toDTO(reviewRepository.save(review), userId);
    }

    @Transactional
    public void deleteReview(UUID userId, UUID reviewId, boolean isModerator) {
        Review review = findById(reviewId);

        if (!isModerator && !review.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Вы не можете удалить чужую рецензию");
        }

        // Clean up dangling likes (the polymorphic table has no FK).
        likeService.deleteFor(LikeService.TARGET_REVIEW, reviewId);
        commentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId)
                .forEach(c -> likeService.deleteFor(LikeService.TARGET_COMMENT, c.getId()));

        reviewRepository.delete(review);
        log.info("Рецензия удалена: id={}, isModerator={}", reviewId, isModerator);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDTO> getBookReviews(UUID bookId, UUID viewerId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByBookIdAndStatus(bookId, "APPROVED", pageable);
        Set<UUID> likedReviews = likeService.findLikedIds(viewerId, LikeService.TARGET_REVIEW,
                reviews.stream().map(Review::getId).collect(Collectors.toList()));

        return reviews.map(r -> {
            ReviewDTO dto = toDTO(r, likedReviews.contains(r.getId()),
                    likeService.count(LikeService.TARGET_REVIEW, r.getId()));
            List<Comment> comments = commentRepository.findByReviewIdOrderByCreatedAtAsc(r.getId());
            Set<UUID> likedComments = likeService.findLikedIds(viewerId, LikeService.TARGET_COMMENT,
                    comments.stream().map(Comment::getId).collect(Collectors.toList()));
            dto.setComments(comments.stream()
                    .map(c -> toCommentDTO(c, likedComments.contains(c.getId()),
                            likeService.count(LikeService.TARGET_COMMENT, c.getId())))
                    .collect(Collectors.toList()));
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public Page<ReviewDTO> getUserReviews(UUID userId, Pageable pageable) {
        return reviewRepository.findByUserId(userId, pageable).map(r -> toDTO(r, userId));
    }

    @Transactional(readOnly = true)
    public Page<ReviewDTO> getPendingReviews(Pageable pageable) {
        return reviewRepository.findByStatus("PENDING", pageable).map(r -> toDTO(r, null));
    }

    @Transactional
    public ReviewDTO approveReview(UUID reviewId) {
        Review review = findById(reviewId);
        review.setStatus("APPROVED");
        ReviewDTO result = toDTO(reviewRepository.save(review), null);
        log.info("Рецензия одобрена модератором: id={}", reviewId);
        return result;
    }

    @Transactional
    public ReviewDTO rejectReview(UUID reviewId) {
        Review review = findById(reviewId);
        review.setStatus("REJECTED");
        ReviewDTO result = toDTO(reviewRepository.save(review), null);
        log.info("Рецензия отклонена модератором: id={}", reviewId);
        return result;
    }

    private Review findById(UUID id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Рецензия не найдена"));
    }

    public ReviewDTO toDTO(Review r, UUID viewerId) {
        long count = likeService.count(LikeService.TARGET_REVIEW, r.getId());
        boolean liked = viewerId != null
                && !likeService.findLikedIds(viewerId, LikeService.TARGET_REVIEW, List.of(r.getId())).isEmpty();
        return toDTO(r, liked, count);
    }

    public ReviewDTO toDTO(Review r, boolean liked, long likes) {
        return ReviewDTO.builder()
                .id(r.getId())
                .bookId(r.getBook().getId())
                .bookTitle(r.getBook().getTitle())
                .author(r.getBook().getAuthor())
                .genre(r.getBook().getGenre())
                .coverUrl(r.getBook().getCoverUrl())
                .userId(r.getUser().getId())
                .userName(r.getUser().getLogin())
                .userAvatar(r.getUser().getAvatarUrl())
                .rating(r.getRating())
                .text(r.getText())
                .status(r.getStatus())
                .likes(likes)
                .liked(liked)
                .createdAt(r.getCreatedAt())
                .reviewAuthor(r.getUser().getLogin())
                .reviewAuthorName(buildFullName(r.getUser()))
                .build();
    }

    private CommentDTO toCommentDTO(Comment c, boolean liked, long likes) {
        return CommentDTO.builder()
                .id(c.getId())
                .userId(c.getUser().getId())
                .userName(c.getUser().getLogin())
                .userAvatar(c.getUser().getAvatarUrl())
                .text(c.getText())
                .likes(likes)
                .liked(liked)
                .createdAt(c.getCreatedAt())
                .build();
    }

    private String buildFullName(User user) {
        StringBuilder sb = new StringBuilder();
        if (user.getLastName() != null) sb.append(user.getLastName()).append(" ");
        if (user.getFirstName() != null) sb.append(user.getFirstName());
        return sb.toString().trim();
    }
}
