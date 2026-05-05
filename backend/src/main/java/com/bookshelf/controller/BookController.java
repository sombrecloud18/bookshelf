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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
            @RequestParam(name = "includeArchived", required = false, defaultValue = "false") boolean includeArchivedParam,
            @PageableDefault(size = 20) Pageable pageable) {
        // Only moderators may pass `includeArchived=true`. For regular users we silently force `false`.
        boolean includeArchived = includeArchivedParam && isModerator();
        if (query != null && !query.isBlank()) {
            log.debug("Поиск книг: query='{}', includeArchived={}, page={}",
                    query, includeArchived, pageable.getPageNumber());
            return ResponseEntity.ok(bookService.searchBooks(query, includeArchived, pageable));
        }
        log.debug("Список книг: includeArchived={}, page={}, size={}",
                includeArchived, pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(bookService.getAllBooks(includeArchived, pageable));
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
        return ResponseEntity.ok(bookService.createBook(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<BookDTO> updateBook(@PathVariable UUID id, @Valid @RequestBody CreateBookDTO dto) {
        log.info("Обновление книги: id={}", id);
        return ResponseEntity.ok(bookService.updateBook(id, dto));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<BookDTO> archive(@PathVariable UUID id) {
        log.info("Архивация книги: id={}", id);
        return ResponseEntity.ok(bookService.setStatus(id, BookService.STATUS_ARCHIVED));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<BookDTO> restore(@PathVariable UUID id) {
        log.info("Восстановление книги: id={}", id);
        return ResponseEntity.ok(bookService.setStatus(id, BookService.STATUS_ACTIVE));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> deleteBook(@PathVariable UUID id) {
        log.info("Удаление книги: id={}", id);
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isModerator() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_MODERATOR"));
    }
}
