package com.bookshelf.dto.auth;

import com.bookshelf.dto.user.UserProfileDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private String role;
    private UserProfileDTO user;
}
