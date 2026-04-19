package com.bookshelf.repository;

import com.bookshelf.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByBookIdAndStatus(UUID bookId, String status, Pageable pageable);
    Page<Review> findByUserId(UUID userId, Pageable pageable);
    Page<Review> findByStatus(String status, Pageable pageable);
    Optional<Review> findByBookIdAndUserId(UUID bookId, UUID userId);
    boolean existsByBookIdAndUserId(UUID bookId, UUID userId);
    long countByStatus(String status);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book.id = :bookId AND r.status = 'APPROVED'")
    Double getAverageRatingByBookId(@Param("bookId") UUID bookId);
}
