package com.bookshelf.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A subject is identified globally by its {@code name}; the same subject
 * may be offered by many specialties. When two specialties share a subject
 * the underlying subject record is the same — collections by subject end
 * up shared (per product spec).
 */
@Entity
@Table(name = "subjects")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255, unique = true)
    private String name;

    /** When true, the subject is implicitly offered by every specialty (e.g. Math, Philosophy). */
    @Column(name = "is_common", nullable = false)
    @Builder.Default
    private boolean isCommon = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "subject_specialties",
               joinColumns = @JoinColumn(name = "subject_id"),
               inverseJoinColumns = @JoinColumn(name = "specialty_id"))
    @Builder.Default
    private Set<Specialty> specialties = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
