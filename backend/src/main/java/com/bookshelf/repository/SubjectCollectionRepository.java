package com.bookshelf.repository;

import com.bookshelf.entity.SubjectCollection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubjectCollectionRepository extends JpaRepository<SubjectCollection, UUID> {

    Page<SubjectCollection> findBySubjectAndSpecialtyAndStatus(String subject, String specialty, String status, Pageable pageable);
    Page<SubjectCollection> findByStatus(String status, Pageable pageable);
    List<SubjectCollection> findByUserId(UUID userId);
    long countByStatus(String status);
}
