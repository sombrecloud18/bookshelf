package com.bookshelf.repository;

import com.bookshelf.entity.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CollectionRepository extends JpaRepository<Collection, UUID> {

    List<Collection> findByUserId(UUID userId);
    Page<Collection> findByStatus(String status, Pageable pageable);
    long countByStatus(String status);

    @Query("SELECT c FROM Collection c WHERE c.status = 'APPROVED' AND " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           " LOWER(c.description) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           " LOWER(c.genre) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Collection> searchByKeyword(@Param("q") String query, Pageable pageable);
}
