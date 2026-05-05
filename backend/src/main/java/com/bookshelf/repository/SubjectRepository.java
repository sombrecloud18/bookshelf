package com.bookshelf.repository;

import com.bookshelf.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    List<Subject> findBySpecialtyOrderByNameAsc(String specialty);

    List<Subject> findAllByOrderBySpecialtyAscNameAsc();

    Optional<Subject> findBySpecialtyAndName(String specialty, String name);

    boolean existsBySpecialtyAndName(String specialty, String name);
}
