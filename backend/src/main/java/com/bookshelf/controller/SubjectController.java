package com.bookshelf.controller;

import com.bookshelf.dto.subject.CreateSubjectDTO;
import com.bookshelf.dto.subject.SubjectDTO;
import com.bookshelf.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@Slf4j
public class SubjectController {

    private final SubjectService subjectService;

    /**
     * Public — list of subjects. {@code specialty} accepts either a specialty UUID
     * (preferred) or a specialty name (legacy clients). When omitted, returns all subjects.
     */
    @GetMapping
    public ResponseEntity<List<SubjectDTO>> list(@RequestParam(required = false) String specialty) {
        log.debug("Список предметов: specialty='{}'", specialty);
        return ResponseEntity.ok(subjectService.listForSpecialty(specialty));
    }

    @PostMapping
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<SubjectDTO> create(@Valid @RequestBody CreateSubjectDTO dto) {
        log.info("Создание предмета: specialty='{}', name='{}'", dto.getSpecialty(), dto.getName());
        return ResponseEntity.ok(subjectService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<SubjectDTO> update(@PathVariable UUID id, @Valid @RequestBody CreateSubjectDTO dto) {
        log.info("Обновление предмета: id={}", id);
        return ResponseEntity.ok(subjectService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Удаление предмета: id={}", id);
        subjectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
