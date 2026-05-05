package com.bookshelf.controller;

import com.bookshelf.dto.user.UserProfileDTO;
import com.bookshelf.service.AdminDashboardService;
import com.bookshelf.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MODERATOR')")
@Slf4j
public class AdminController {

    private final UserService userService;
    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        log.debug("Запрос статистики дашборда");
        Map<String, Object> stats = adminDashboardService.getDashboardStats();
        log.debug("Статистика дашборда: {}", stats);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserProfileDTO>> getAllUsers(@PageableDefault(size = 20) Pageable pageable) {
        log.debug("Список всех пользователей: page={}", pageable.getPageNumber());
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @PostMapping("/users/{id}/block")
    public ResponseEntity<Void> blockUser(@PathVariable UUID id) {
        log.info("Блокировка пользователя: id={}", id);
        userService.blockUser(id);
        log.info("Пользователь заблокирован: id={}", id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/unblock")
    public ResponseEntity<Void> unblockUser(@PathVariable UUID id) {
        log.info("Разблокировка пользователя: id={}", id);
        userService.unblockUser(id);
        log.info("Пользователь разблокирован: id={}", id);
        return ResponseEntity.ok().build();
    }
}
