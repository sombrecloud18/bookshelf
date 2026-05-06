package com.bookshelf.repository;

import com.bookshelf.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    List<Subject> findAllByOrderByNameAsc();

    Optional<Subject> findByName(String name);

    boolean existsByName(String name);

    /**
     * Subjects offered to a given specialty: either explicitly linked via the
     * subject_specialties M2M, or marked as common (offered everywhere).
     */
    @Query("""
            SELECT DISTINCT s FROM Subject s
            LEFT JOIN s.specialties sp
            WHERE s.isCommon = true OR sp.id = :specialtyId
            ORDER BY s.name ASC
            """)
    List<Subject> findForSpecialty(@Param("specialtyId") UUID specialtyId);
}
