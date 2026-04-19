package com.bookshelf.controller;

import com.bookshelf.dto.book.BookDTO;
import com.bookshelf.dto.order.CreateOrderDTO;
import com.bookshelf.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<BookDTO> addToOrder(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateOrderDTO dto) {
        return ResponseEntity.ok(orderService.addToOrder(userId, dto.getBookId()));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeFromOrder(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID bookId) {
        orderService.removeFromOrder(userId, bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<BookDTO>> getMyOrders(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    @GetMapping("/check/{bookId}")
    public ResponseEntity<Map<String, Boolean>> checkOrder(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID bookId) {
        return ResponseEntity.ok(Map.of("hasOrder", orderService.hasOrder(userId, bookId)));
    }
}
