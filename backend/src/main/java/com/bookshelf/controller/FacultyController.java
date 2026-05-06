package com.bookshelf.controller;

import com.bookshelf.dto.study.CreateFacultyDTO;
import com.bookshelf.dto.study.CreateSpecialtyDTO;
import com.bookshelf.dto.study.FacultyDTO;
import com.bookshelf.dto.study.SpecialtyDTO;
import com.bookshelf.service.FacultyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class FacultyController {

    private final FacultyService facultyService;

    /** Public read of the full faculty/specialty tree — needed by registration, account, subjects tab. */
    @GetMapping("/api/faculties")
    public ResponseEntity<List<FacultyDTO>> list() {
        return ResponseEntity.ok(facultyService.listAll());
    }

    @PostMapping("/api/admin/faculties")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<FacultyDTO> createFaculty(@Valid @RequestBody CreateFacultyDTO dto) {
        log.info("Создание факультета: name='{}'", dto.getName());
        return ResponseEntity.ok(facultyService.createFaculty(dto));
    }

    @PutMapping("/api/admin/faculties/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<FacultyDTO> renameFaculty(@PathVariable UUID id,
                                                     @Valid @RequestBody CreateFacultyDTO dto) {
        log.info("Переименование факультета: id={}, name='{}'", id, dto.getName());
        return ResponseEntity.ok(facultyService.renameFaculty(id, dto));
    }

    @DeleteMapping("/api/admin/faculties/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> deleteFaculty(@PathVariable UUID id) {
        log.info("Удаление факультета: id={}", id);
        facultyService.deleteFaculty(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/specialties")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<SpecialtyDTO> createSpecialty(@Valid @RequestBody CreateSpecialtyDTO dto) {
        log.info("Создание специальности: facultyId={}, name='{}'", dto.getFacultyId(), dto.getName());
        return ResponseEntity.ok(facultyService.createSpecialty(dto));
    }

    @PutMapping("/api/admin/specialties/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<SpecialtyDTO> renameSpecialty(@PathVariable UUID id,
                                                         @Valid @RequestBody CreateSpecialtyDTO dto) {
        log.info("Обновление специальности: id={}", id);
        return ResponseEntity.ok(facultyService.renameSpecialty(id, dto));
    }

    @DeleteMapping("/api/admin/specialties/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> deleteSpecialty(@PathVariable UUID id) {
        log.info("Удаление специальности: id={}", id);
        facultyService.deleteSpecialty(id);
        return ResponseEntity.noContent().build();
    }
}
