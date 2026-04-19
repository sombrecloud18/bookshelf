package com.bookshelf.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDTO {
    @NotBlank(message = "Логин обязателен")
    private String login;

    @NotBlank(message = "Пароль обязателен")
    private String password;
}
