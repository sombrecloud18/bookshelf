package com.bookshelf.repository;

import com.bookshelf.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpecialtyRepository extends JpaRepository<Specialty, UUID> {

    List<Specialty> findByFacultyIdOrderByNameAsc(UUID facultyId);

    boolean existsByFacultyIdAndName(UUID facultyId, String name);

    Optional<Specialty> findByFacultyIdAndName(UUID facultyId, String name);

    List<Specialty> findByName(String name);
}
