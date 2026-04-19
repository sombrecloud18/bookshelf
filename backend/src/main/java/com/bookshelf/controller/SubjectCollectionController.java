package com.bookshelf.controller;

import com.bookshelf.dto.collection.CreateSubjectCollectionDTO;
import com.bookshelf.dto.collection.SubjectCollectionDTO;
import com.bookshelf.service.SubjectCollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/subject-collections")
@RequiredArgsConstructor
public class SubjectCollectionController {

    private final SubjectCollectionService subjectCollectionService;

    @PostMapping
    public ResponseEntity<SubjectCollectionDTO> createCollection(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateSubjectCollectionDTO dto) {
        return ResponseEntity.ok(subjectCollectionService.createSubjectCollection(userId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubjectCollectionDTO> updateCollection(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateSubjectCollectionDTO dto) {
        return ResponseEntity.ok(subjectCollectionService.updateSubjectCollection(userId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollection(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        boolean isModerator = isModerator();
        subjectCollectionService.deleteSubjectCollection(userId, id, isModerator);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<SubjectCollectionDTO>> getApprovedCollections(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String specialty,
            @PageableDefault(size = 12) Pageable pageable) {
        return ResponseEntity.ok(subjectCollectionService.getApprovedCollections(subject, specialty, pageable));
    }

    @GetMapping("/my")
    public ResponseEntity<List<SubjectCollectionDTO>> getMyCollections(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(subjectCollectionService.getUserCollections(userId));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Page<SubjectCollectionDTO>> getPendingCollections(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(subjectCollectionService.getPendingCollections(pageable));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<SubjectCollectionDTO> approveCollection(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String comment = body != null ? body.get("moderatorComment") : null;
        return ResponseEntity.ok(subjectCollectionService.approveCollection(id, comment));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<SubjectCollectionDTO> rejectCollection(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String comment = body != null ? body.get("moderatorComment") : null;
        return ResponseEntity.ok(subjectCollectionService.rejectCollection(id, comment));
    }

    private boolean isModerator() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_MODERATOR"));
    }
}
