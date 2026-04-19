package com.bookshelf.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "books")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(length = 100)
    private String genre;

    @Column
    private Integer year;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "full_description", columnDefinition = "TEXT")
    private String fullDescription;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column
    private Integer pages;

    @Column(length = 255)
    private String publisher;

    @Column(name = "publish_year")
    private Integer publishYear;

    @Column(unique = true, length = 20)
    private String isbn;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
