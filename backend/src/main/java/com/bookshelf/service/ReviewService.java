package com.bookshelf.service;

import com.bookshelf.dto.comment.CommentDTO;
import com.bookshelf.dto.review.CreateReviewDTO;
import com.bookshelf.dto.review.ReviewDTO;
import com.bookshelf.dto.review.UpdateReviewDTO;
import com.bookshelf.entity.*;
import com.bookshelf.exception.AppException;
import com.bookshelf.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final UserActivityRepository userActivityRepository;

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
                .likes(0)
                .build();

        review = reviewRepository.save(review);

        // Record activity
        userActivityRepository.save(UserActivity.builder()
                .user(user).book(book).activityType("REVIEW").build());

        return toDTO(review);
    }

    @Transactional
    public ReviewDTO updateReview(UUID userId, UUID reviewId, UpdateReviewDTO dto) {
        Review review = findById(reviewId);

        if (!review.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Вы не можете редактировать чужую рецензию");
        }

        review.setRating(dto.getRating());
        review.setText(dto.getText());
        review.setStatus("PENDING"); // Re-moderation required

        return toDTO(reviewRepository.save(review));
    }

    @Transactional
    public void deleteReview(UUID userId, UUID reviewId, boolean isModerator) {
        Review review = findById(reviewId);

        if (!isModerator && !review.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Вы не можете удалить чужую рецензию");
        }

        reviewRepository.delete(review);
    }

    public Page<ReviewDTO> getBookReviews(UUID bookId, Pageable pageable) {
        return reviewRepository.findByBookIdAndStatus(bookId, "APPROVED", pageable)
                .map(r -> {
                    ReviewDTO dto = toDTO(r);
                    List<Comment> comments = commentRepository.findByReviewIdOrderByCreatedAtAsc(r.getId());
                    dto.setComments(comments.stream().map(this::toCommentDTO).collect(Collectors.toList()));
                    return dto;
                });
    }

    public Page<ReviewDTO> getUserReviews(UUID userId, Pageable pageable) {
        return reviewRepository.findByUserId(userId, pageable).map(this::toDTO);
    }

    public Page<ReviewDTO> getPendingReviews(Pageable pageable) {
        return reviewRepository.findByStatus("PENDING", pageable).map(this::toDTO);
    }

    @Transactional
    public ReviewDTO approveReview(UUID reviewId) {
        Review review = findById(reviewId);
        review.setStatus("APPROVED");
        return toDTO(reviewRepository.save(review));
    }

    @Transactional
    public ReviewDTO rejectReview(UUID reviewId) {
        Review review = findById(reviewId);
        review.setStatus("REJECTED");
        return toDTO(reviewRepository.save(review));
    }

    @Transactional
    public void likeReview(UUID reviewId) {
        Review review = findById(reviewId);
        review.setLikes(review.getLikes() + 1);
        reviewRepository.save(review);
    }

    private Review findById(UUID id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Рецензия не найдена"));
    }

    public ReviewDTO toDTO(Review r) {
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
                .likes(r.getLikes())
                .createdAt(r.getCreatedAt())
                .reviewAuthor(r.getUser().getLogin())
                .reviewAuthorName(buildFullName(r.getUser()))
                .build();
    }

    private CommentDTO toCommentDTO(Comment c) {
        return CommentDTO.builder()
                .id(c.getId())
                .userId(c.getUser().getId())
                .userName(c.getUser().getLogin())
                .userAvatar(c.getUser().getAvatarUrl())
                .text(c.getText())
                .likes(c.getLikes())
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
