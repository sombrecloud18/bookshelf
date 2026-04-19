package com.bookshelf.service;

import com.bookshelf.dto.book.BookDTO;
import com.bookshelf.dto.book.CreateBookDTO;
import com.bookshelf.entity.Book;
import com.bookshelf.exception.AppException;
import com.bookshelf.repository.BookRepository;
import com.bookshelf.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;

    public Page<BookDTO> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable).map(this::toDTO);
    }

    public BookDTO getBookById(UUID id) {
        Book book = findById(id);
        Double avgRating = reviewRepository.getAverageRatingByBookId(id);
        BookDTO dto = toDTO(book);
        dto.setAverageRating(avgRating);
        return dto;
    }

    public Page<BookDTO> searchBooks(String query, Pageable pageable) {
        if (!StringUtils.hasText(query)) {
            return getAllBooks(pageable);
        }

        // Try full-text search first
        Page<Book> results = bookRepository.searchByText(query, pageable);
        if (results.getTotalElements() == 0) {
            // Fallback to ILIKE search
            results = bookRepository.searchByTitleOrAuthor(query, pageable);
        }
        return results.map(this::toDTO);
    }

    public List<BookDTO> getBooksByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return bookRepository.findByIdIn(ids).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookDTO createBook(CreateBookDTO dto) {
        Book book = Book.builder()
                .title(dto.getTitle())
                .author(dto.getAuthor())
                .genre(dto.getGenre())
                .year(dto.getYear())
                .description(dto.getDescription())
                .fullDescription(dto.getFullDescription())
                .imageUrl(dto.getImageUrl())
                .coverUrl(dto.getCoverUrl())
                .pages(dto.getPages())
                .publisher(dto.getPublisher())
                .publishYear(dto.getPublishYear())
                .isbn(dto.getIsbn())
                .build();
        return toDTO(bookRepository.save(book));
    }

    @Transactional
    public BookDTO updateBook(UUID id, CreateBookDTO dto) {
        Book book = findById(id);
        if (dto.getTitle() != null) book.setTitle(dto.getTitle());
        if (dto.getAuthor() != null) book.setAuthor(dto.getAuthor());
        if (dto.getGenre() != null) book.setGenre(dto.getGenre());
        if (dto.getYear() != null) book.setYear(dto.getYear());
        if (dto.getDescription() != null) book.setDescription(dto.getDescription());
        if (dto.getFullDescription() != null) book.setFullDescription(dto.getFullDescription());
        if (dto.getImageUrl() != null) book.setImageUrl(dto.getImageUrl());
        if (dto.getCoverUrl() != null) book.setCoverUrl(dto.getCoverUrl());
        if (dto.getPages() != null) book.setPages(dto.getPages());
        if (dto.getPublisher() != null) book.setPublisher(dto.getPublisher());
        if (dto.getPublishYear() != null) book.setPublishYear(dto.getPublishYear());
        if (dto.getIsbn() != null) book.setIsbn(dto.getIsbn());
        return toDTO(bookRepository.save(book));
    }

    @Transactional
    public void deleteBook(UUID id) {
        if (!bookRepository.existsById(id)) {
            throw AppException.notFound("Книга не найдена");
        }
        bookRepository.deleteById(id);
    }

    public Book findById(UUID id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Книга не найдена"));
    }

    public BookDTO toDTO(Book book) {
        return BookDTO.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .genre(book.getGenre())
                .year(book.getYear())
                .description(book.getDescription())
                .fullDescription(book.getFullDescription())
                .imageUrl(book.getImageUrl())
                .coverUrl(book.getCoverUrl())
                .pages(book.getPages())
                .publisher(book.getPublisher())
                .publishYear(book.getPublishYear())
                .isbn(book.getIsbn())
                .build();
    }
}
