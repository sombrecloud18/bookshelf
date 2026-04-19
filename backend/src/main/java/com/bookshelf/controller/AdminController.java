package com.bookshelf.controller;

import com.bookshelf.dto.user.UserProfileDTO;
import com.bookshelf.service.AdminDashboardService;
import com.bookshelf.service.UserService;
import lombok.RequiredArgsConstructor;
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
public class AdminController {

    private final UserService userService;
    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(adminDashboardService.getDashboardStats());
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserProfileDTO>> getAllUsers(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @PostMapping("/users/{id}/block")
    public ResponseEntity<Void> blockUser(@PathVariable UUID id) {
        userService.blockUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/unblock")
    public ResponseEntity<Void> unblockUser(@PathVariable UUID id) {
        userService.unblockUser(id);
        return ResponseEntity.ok().build();
    }
}
