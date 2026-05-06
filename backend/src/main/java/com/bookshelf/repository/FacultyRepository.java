package com.bookshelf.repository;

import com.bookshelf.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FacultyRepository extends JpaRepository<Faculty, UUID> {

    List<Faculty> findAllByOrderByNameAsc();

    Optional<Faculty> findByName(String name);

    boolean existsByName(String name);
}
