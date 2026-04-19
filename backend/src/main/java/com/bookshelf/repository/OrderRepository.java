package com.bookshelf.repository;

import com.bookshelf.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserId(UUID userId);
    boolean existsByUserIdAndBookId(UUID userId, UUID bookId);
    Optional<Order> findByUserIdAndBookId(UUID userId, UUID bookId);

    @Modifying
    @Query("DELETE FROM Order o WHERE o.user.id = :userId AND o.book.id = :bookId")
    void deleteByUserIdAndBookId(@Param("userId") UUID userId, @Param("bookId") UUID bookId);
}
