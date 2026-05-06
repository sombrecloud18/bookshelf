package com.bookshelf.dto.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubjectDTO {
    private UUID id;
    private String name;
    /** When true the subject is offered by every specialty implicitly. */
    private boolean common;
    /** Specialties this subject is explicitly linked to (empty when {@code common} is true). */
    private List<UUID> specialtyIds;
    /** Pretty list of "Faculty / Specialty" strings — handy in the admin UI. */
    private List<String> specialtyLabels;
}
