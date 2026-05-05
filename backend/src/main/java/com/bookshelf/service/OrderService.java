package com.bookshelf.service;

import com.bookshelf.dto.book.BookDTO;
import com.bookshelf.entity.Book;
import com.bookshelf.entity.Order;
import com.bookshelf.entity.User;
import com.bookshelf.entity.UserActivity;
import com.bookshelf.exception.AppException;
import com.bookshelf.repository.BookRepository;
import com.bookshelf.repository.OrderRepository;
import com.bookshelf.repository.UserActivityRepository;
import com.bookshelf.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final UserActivityRepository userActivityRepository;
    private final BookService bookService;

    @Transactional
    public BookDTO addToOrder(UUID userId, UUID bookId) {
        log.debug("Добавление книги в список чтения: userId={}, bookId={}", userId, bookId);
        if (orderRepository.existsByUserIdAndBookId(userId, bookId)) {
            log.warn("Книга уже в списке чтения: userId={}, bookId={}", userId, bookId);
            throw AppException.conflict("Книга уже в вашем читательском списке");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("Пользователь не найден"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> AppException.notFound("Книга не найдена"));

        Order order = Order.builder()
                .user(user)
                .book(book)
                .status("ACTIVE")
                .build();

        orderRepository.save(order);
        log.info("Книга добавлена в список чтения: userId={}, bookId={}, bookTitle='{}'", userId, bookId, book.getTitle());

        userActivityRepository.save(UserActivity.builder()
                .user(user).book(book).activityType("ORDER").build());

        return bookService.toDTO(book);
    }

    @Transactional
    public void removeFromOrder(UUID userId, UUID bookId) {
        log.debug("Удаление книги из списка чтения: userId={}, bookId={}", userId, bookId);
        if (!orderRepository.existsByUserIdAndBookId(userId, bookId)) {
            throw AppException.notFound("Книга не найдена в вашем читательском списке");
        }
        orderRepository.deleteByUserIdAndBookId(userId, bookId);
        log.info("Книга удалена из списка чтения: userId={}, bookId={}", userId, bookId);
    }

    @Transactional(readOnly = true)
    public List<BookDTO> getUserOrders(UUID userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(o -> bookService.toDTO(o.getBook()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean hasOrder(UUID userId, UUID bookId) {
        return orderRepository.existsByUserIdAndBookId(userId, bookId);
    }
}
