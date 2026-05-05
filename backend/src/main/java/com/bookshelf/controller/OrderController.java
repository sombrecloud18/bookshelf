package com.bookshelf.controller;

import com.bookshelf.dto.book.BookDTO;
import com.bookshelf.dto.order.CreateOrderDTO;
import com.bookshelf.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<BookDTO> addToOrder(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateOrderDTO dto) {
        log.info("Добавление книги в список чтения: userId={}, bookId={}", userId, dto.getBookId());
        BookDTO result = orderService.addToOrder(userId, dto.getBookId());
        log.info("Книга добавлена в список чтения: userId={}, bookId={}", userId, dto.getBookId());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeFromOrder(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID bookId) {
        log.info("Удаление книги из списка чтения: userId={}, bookId={}", userId, bookId);
        orderService.removeFromOrder(userId, bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<BookDTO>> getMyOrders(@AuthenticationPrincipal UUID userId) {
        log.debug("Список чтения пользователя: userId={}", userId);
        List<BookDTO> orders = orderService.getUserOrders(userId);
        log.debug("Список чтения получен: userId={}, count={}", userId, orders.size());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/check/{bookId}")
    public ResponseEntity<Map<String, Boolean>> checkOrder(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID bookId) {
        log.debug("Проверка наличия книги в списке: userId={}, bookId={}", userId, bookId);
        return ResponseEntity.ok(Map.of("hasOrder", orderService.hasOrder(userId, bookId)));
    }
}
