package com.bookshelf.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "collection_books")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionBook {

    @EmbeddedId
    private CollectionBookId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("collectionId")
    @JoinColumn(name = "collection_id")
    private Collection collection;

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
    public static class CollectionBookId implements Serializable {
        @Column(name = "collection_id")
        private UUID collectionId;

        @Column(name = "book_id")
        private UUID bookId;
    }
}
