package com.bookshelf.controller;

import com.bookshelf.dto.book.BookDTO;
import com.bookshelf.dto.book.BookIdsRequestDTO;
import com.bookshelf.dto.book.CreateBookDTO;
import com.bookshelf.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<Page<BookDTO>> getBooks(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20) Pageable pageable) {
        if (query != null && !query.isBlank()) {
            return ResponseEntity.ok(bookService.searchBooks(query, pageable));
        }
        return ResponseEntity.ok(bookService.getAllBooks(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBook(@PathVariable UUID id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @PostMapping("/by-ids")
    public ResponseEntity<List<BookDTO>> getBooksByIds(@RequestBody BookIdsRequestDTO dto) {
        return ResponseEntity.ok(bookService.getBooksByIds(dto.getIds()));
    }

    @PostMapping
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<BookDTO> createBook(@Valid @RequestBody CreateBookDTO dto) {
        return ResponseEntity.ok(bookService.createBook(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<BookDTO> updateBook(@PathVariable UUID id, @Valid @RequestBody CreateBookDTO dto) {
        return ResponseEntity.ok(bookService.updateBook(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> deleteBook(@PathVariable UUID id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
