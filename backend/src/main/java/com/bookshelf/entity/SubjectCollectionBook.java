package com.bookshelf.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "subject_collection_books")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectCollectionBook {

    @EmbeddedId
    private SubjectCollectionBookId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("subjectCollectionId")
    @JoinColumn(name = "subject_collection_id")
    private SubjectCollection subjectCollection;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("bookId")
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(nullable = false)
    private Integer position = 0;

    @Embeddable
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class SubjectCollectionBookId implements Serializable {
        @Column(name = "subject_collection_id")
        private UUID subjectCollectionId;

        @Column(name = "book_id")
        private UUID bookId;
    }
}
