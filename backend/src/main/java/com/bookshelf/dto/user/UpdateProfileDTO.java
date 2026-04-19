package com.bookshelf.dto.user;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateProfileDTO {
    private String firstName;
    private String lastName;
    private String patronymic;
    private String faculty;
    private String specialty;
    private String course;
    private String phoneNumber;
    @Email(message = "Некорректный формат email")
    private String email;
    private String avatarUrl;
}
