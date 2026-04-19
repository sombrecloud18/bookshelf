package com.bookshelf.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserProfileDTO {
    private UUID id;
    private String login;
    private String email;
    private String firstName;
    private String lastName;
    private String patronymic;
    private String faculty;
    private String specialty;
    private String course;
    private String phoneNumber;
    private String avatarUrl;
    private String role;
    private String fullName;
    private String studyInfo;
    private boolean isBlocked;
}
