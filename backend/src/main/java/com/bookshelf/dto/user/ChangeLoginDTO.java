package com.bookshelf.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangeLoginDTO {
    @NotBlank(message = "Текущий пароль обязателен")
    private String currentPassword;

    @NotBlank(message = "Новый логин обязателен")
    @Size(min = 3, max = 100, message = "Логин должен содержать от 3 до 100 символов")
    private String newLogin;
}
