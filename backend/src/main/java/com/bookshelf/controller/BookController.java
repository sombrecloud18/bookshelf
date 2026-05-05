package com.bookshelf.controller;

import com.bookshelf.dto.book.BookDTO;
import com.bookshelf.dto.book.BookIdsRequestDTO;
import com.bookshelf.dto.book.CreateBookDTO;
import com.bookshelf.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Slf4j
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<Page<BookDTO>> getBooks(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20) Pageable pageable) {
        if (query != null && !query.isBlank()) {
            log.debug("Поиск книг: query='{}', page={}", query, pageable.getPageNumber());
            return ResponseEntity.ok(bookService.searchBooks(query, pageable));
        }
        log.debug("Список всех книг: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(bookService.getAllBooks(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBook(@PathVariable UUID id) {
        log.debug("Получение книги: id={}", id);
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @PostMapping("/by-ids")
    public ResponseEntity<List<BookDTO>> getBooksByIds(@RequestBody BookIdsRequestDTO dto) {
        log.debug("Получение книг по списку id: count={}", dto.getIds() != null ? dto.getIds().size() : 0);
        return ResponseEntity.ok(bookService.getBooksByIds(dto.getIds()));
    }

    @PostMapping
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<BookDTO> createBook(@Valid @RequestBody CreateBookDTO dto) {
        log.info("Создание книги: title='{}', author='{}'", dto.getTitle(), dto.getAuthor());
        BookDTO result = bookService.createBook(dto);
        log.info("Книга создана: id={}, title='{}'", result.getId(), result.getTitle());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<BookDTO> updateBook(@PathVariable UUID id, @Valid @RequestBody CreateBookDTO dto) {
        log.info("Обновление книги: id={}", id);
        BookDTO result = bookService.updateBook(id, dto);
        log.info("Книга обновлена: id={}", id);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> deleteBook(@PathVariable UUID id) {
        log.info("Удаление книги: id={}", id);
        bookService.deleteBook(id);
        log.info("Книга удалена: id={}", id);
        return ResponseEntity.noContent().build();
    }
}
