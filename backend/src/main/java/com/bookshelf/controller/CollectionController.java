package com.bookshelf.controller;

import com.bookshelf.dto.collection.CollectionDTO;
import com.bookshelf.dto.collection.CreateCollectionDTO;
import com.bookshelf.service.CollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class CollectionController {

    private final CollectionService collectionService;

    @PostMapping
    public ResponseEntity<CollectionDTO> createCollection(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateCollectionDTO dto) {
        log.info("Создание подборки: userId={}, title='{}'", userId, dto.getTitle());
        CollectionDTO result = collectionService.createCollection(userId, dto);
        log.info("Подборка создана: id={}, userId={}", result.getId(), userId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CollectionDTO> updateCollection(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateCollectionDTO dto) {
        log.info("Обновление подборки: id={}, userId={}", id, userId);
        CollectionDTO result = collectionService.updateCollection(userId, id, dto);
        log.info("Подборка обновлена: id={}", id);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollection(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        boolean isModerator = isModerator();
        log.info("Удаление подборки: id={}, userId={}, isModerator={}", id, userId, isModerator);
        collectionService.deleteCollection(userId, id, isModerator);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<CollectionDTO> publishCollection(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        log.info("Публикация подборки на модерацию: id={}, userId={}", id, userId);
        CollectionDTO result = collectionService.publishCollection(userId, id);
        log.info("Подборка отправлена на модерацию: id={}", id);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/my")
    public ResponseEntity<List<CollectionDTO>> getMyCollections(@AuthenticationPrincipal UUID userId) {
        log.debug("Мои подборки: userId={}", userId);
        List<CollectionDTO> result = collectionService.getUserCollections(userId);
        log.debug("Мои подборки получены: userId={}, count={}", userId, result.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<Page<CollectionDTO>> getApprovedCollections(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 12) Pageable pageable) {
        log.debug("Список подборок: query='{}', page={}", query, pageable.getPageNumber());
        return ResponseEntity.ok(collectionService.getApprovedCollections(query, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollectionDTO> getCollection(@PathVariable UUID id) {
        log.debug("Получение подборки: id={}", id);
        return ResponseEntity.ok(collectionService.toDTO(collectionService.findById(id)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Page<CollectionDTO>> getPendingCollections(@PageableDefault(size = 20) Pageable pageable) {
        log.debug("Подборки на модерации: page={}", pageable.getPageNumber());
        return ResponseEntity.ok(collectionService.getPendingCollections(pageable));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<CollectionDTO> approveCollection(@PathVariable UUID id) {
        log.info("Одобрение подборки: id={}", id);
        return ResponseEntity.ok(collectionService.approveCollection(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<CollectionDTO> rejectCollection(@PathVariable UUID id) {
        log.info("Отклонение подборки: id={}", id);
        return ResponseEntity.ok(collectionService.rejectCollection(id));
    }

    private boolean isModerator() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_MODERATOR"));
    }
}
