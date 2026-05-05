package com.bookshelf.controller;

import com.bookshelf.dto.auth.AuthResponseDTO;
import com.bookshelf.dto.auth.LoginRequestDTO;
import com.bookshelf.dto.auth.RegisterRequestDTO;
import com.bookshelf.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO dto) {
        log.debug("Регистрация нового пользователя: login={}, email={}", dto.getLogin(), dto.getEmail());
        AuthResponseDTO result = userService.register(dto);
        log.info("Пользователь зарегистрирован: login={}", dto.getLogin());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        log.debug("Попытка входа: login={}", dto.getLogin());
        AuthResponseDTO result = userService.login(dto);
        log.info("Успешный вход: login={}, role={}", dto.getLogin(), result.getRole());
        return ResponseEntity.ok(result);
    }
}
