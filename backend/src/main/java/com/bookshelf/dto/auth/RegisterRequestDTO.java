package com.bookshelf.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDTO {
    @NotBlank(message = "Логин обязателен")
    @Size(min = 3, max = 100, message = "Логин должен содержать от 3 до 100 символов")
    private String login;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, message = "Пароль должен содержать не менее 6 символов")
    private String password;

    @NotBlank(message = "Имя обязательно")
    private String firstName;

    @NotBlank(message = "Фамилия обязательна")
    private String lastName;

    private String patronymic;
    private String faculty;
    private String specialty;
    private String course;
    private String phoneNumber;
    private String avatarUrl;
}
