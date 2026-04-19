package com.bookshelf.controller;

import com.bookshelf.dto.collection.CollectionDTO;
import com.bookshelf.dto.collection.CreateCollectionDTO;
import com.bookshelf.service.CollectionService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @PostMapping
    public ResponseEntity<CollectionDTO> createCollection(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateCollectionDTO dto) {
        return ResponseEntity.ok(collectionService.createCollection(userId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CollectionDTO> updateCollection(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateCollectionDTO dto) {
        return ResponseEntity.ok(collectionService.updateCollection(userId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollection(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        boolean isModerator = isModerator();
        collectionService.deleteCollection(userId, id, isModerator);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<CollectionDTO> publishCollection(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(collectionService.publishCollection(userId, id));
    }

    @GetMapping("/my")
    public ResponseEntity<List<CollectionDTO>> getMyCollections(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(collectionService.getUserCollections(userId));
    }

    @GetMapping
    public ResponseEntity<Page<CollectionDTO>> getApprovedCollections(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 12) Pageable pageable) {
        return ResponseEntity.ok(collectionService.getApprovedCollections(query, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollectionDTO> getCollection(@PathVariable UUID id) {
        return ResponseEntity.ok(collectionService.toDTO(collectionService.findById(id)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Page<CollectionDTO>> getPendingCollections(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(collectionService.getPendingCollections(pageable));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<CollectionDTO> approveCollection(@PathVariable UUID id) {
        return ResponseEntity.ok(collectionService.approveCollection(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<CollectionDTO> rejectCollection(@PathVariable UUID id) {
        return ResponseEntity.ok(collectionService.rejectCollection(id));
    }

    private boolean isModerator() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_MODERATOR"));
    }
}
