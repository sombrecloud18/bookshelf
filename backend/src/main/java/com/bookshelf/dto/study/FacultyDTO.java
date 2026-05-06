package com.bookshelf.dto.study;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FacultyDTO {
    private UUID id;
    private String name;
    private List<SpecialtyDTO> specialties;
}
