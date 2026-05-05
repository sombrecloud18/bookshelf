package com.bookshelf.controller;

import com.bookshelf.dto.user.ChangeLoginDTO;
import com.bookshelf.dto.user.ChangePasswordDTO;
import com.bookshelf.dto.user.UpdateProfileDTO;
import com.bookshelf.dto.user.UserProfileDTO;
import com.bookshelf.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getProfile(@AuthenticationPrincipal UUID userId) {
        log.debug("Получение профиля: userId={}", userId);
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDTO> updateProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateProfileDTO dto) {
        log.info("Обновление профиля: userId={}", userId);
        UserProfileDTO result = userService.updateProfile(userId, dto);
        log.info("Профиль обновлён: userId={}", userId);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/me/avatar")
    public ResponseEntity<UserProfileDTO> updateAvatar(
            @AuthenticationPrincipal UUID userId,
            @RequestBody java.util.Map<String, String> body) {
        log.info("Обновление аватара: userId={}", userId);
        return ResponseEntity.ok(userService.updateAvatar(userId, body.get("avatarUrl")));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ChangePasswordDTO dto) {
        log.info("Смена пароля: userId={}", userId);
        userService.changePassword(userId, dto);
        log.info("Пароль изменён: userId={}", userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/login")
    public ResponseEntity<UserProfileDTO> changeLogin(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ChangeLoginDTO dto) {
        log.info("Смена логина: userId={}", userId);
        return ResponseEntity.ok(userService.changeLogin(userId, dto));
    }
}
