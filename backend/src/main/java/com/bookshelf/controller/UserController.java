package com.bookshelf.controller;

import com.bookshelf.dto.user.ChangePasswordDTO;
import com.bookshelf.dto.user.UpdateProfileDTO;
import com.bookshelf.dto.user.UserProfileDTO;
import com.bookshelf.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getProfile(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDTO> updateProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateProfileDTO dto) {
        return ResponseEntity.ok(userService.updateProfile(userId, dto));
    }

    @PatchMapping("/me/avatar")
    public ResponseEntity<UserProfileDTO> updateAvatar(
            @AuthenticationPrincipal UUID userId,
            @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(userService.updateAvatar(userId, body.get("avatarUrl")));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(userId, dto);
        return ResponseEntity.ok().build();
    }
}
