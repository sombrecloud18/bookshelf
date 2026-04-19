package com.bookshelf.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "subject_collections")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String subject;

    @Column(nullable = false, length = 50)
    private String specialty;

    @Column(name = "specialty_name", nullable = false, length = 255)
    private String specialtyName;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "author_role", length = 20)
    private String authorRole;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "moderator_comment", columnDefinition = "TEXT")
    private String moderatorComment;

    @OneToMany(mappedBy = "subjectCollection", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<SubjectCollectionBook> subjectCollectionBooks = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
