package com.bookshelf.service;

import com.bookshelf.dto.book.BookDTO;
import com.bookshelf.dto.book.CreateBookDTO;
import com.bookshelf.entity.Book;
import com.bookshelf.exception.AppException;
import com.bookshelf.repository.BookRepository;
import com.bookshelf.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class BookService {

    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public Page<BookDTO> getAllBooks(Pageable pageable) {
        log.debug("Запрос всех книг: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<BookDTO> result = bookRepository.findAll(pageable).map(this::toDTO);
        log.debug("Книги получены: total={}", result.getTotalElements());
        return result;
    }

    @Transactional(readOnly = true)
    public BookDTO getBookById(UUID id) {
        log.debug("Поиск книги по id={}", id);
        Book book = findById(id);
        Double avgRating = reviewRepository.getAverageRatingByBookId(id);
        log.debug("Книга найдена: title='{}', avgRating={}", book.getTitle(), avgRating);
        BookDTO dto = toDTO(book);
        dto.setAverageRating(avgRating);
        return dto;
    }

    @Transactional(readOnly = true)
    public Page<BookDTO> searchBooks(String query, Pageable pageable) {
        if (!StringUtils.hasText(query)) {
            return getAllBooks(pageable);
        }

        log.debug("Полнотекстовый поиск книг: query='{}'", query);
        Page<Book> results = bookRepository.searchByText(query, pageable);
        if (results.getTotalElements() == 0) {
            log.debug("Полнотекстовый поиск не дал результатов — переход на ILIKE: query='{}'", query);
            results = bookRepository.searchByTitleOrAuthor(query, pageable);
        }
        log.debug("Поиск книг завершён: query='{}', found={}", query, results.getTotalElements());
        return results.map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<BookDTO> getBooksByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        log.debug("Запрос книг по id-списку: count={}", ids.size());
        List<BookDTO> result = bookRepository.findByIdIn(ids).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        log.debug("Книги по id-списку получены: requested={}, found={}", ids.size(), result.size());
        return result;
    }

    @Transactional
    public BookDTO createBook(CreateBookDTO dto) {
        log.debug("Создание книги: title='{}', author='{}'", dto.getTitle(), dto.getAuthor());
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
        BookDTO result = toDTO(bookRepository.save(book));
        log.info("Книга сохранена: id={}, title='{}'", result.getId(), result.getTitle());
        return result;
    }

    @Transactional
    public BookDTO updateBook(UUID id, CreateBookDTO dto) {
        log.debug("Обновление книги: id={}", id);
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
        BookDTO result = toDTO(bookRepository.save(book));
        log.info("Книга обновлена: id={}, title='{}'", result.getId(), result.getTitle());
        return result;
    }

    @Transactional
    public void deleteBook(UUID id) {
        if (!bookRepository.existsById(id)) {
            log.warn("Попытка удалить несуществующую книгу: id={}", id);
            throw AppException.notFound("Книга не найдена");
        }
        bookRepository.deleteById(id);
        log.info("Книга удалена из БД: id={}", id);
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
