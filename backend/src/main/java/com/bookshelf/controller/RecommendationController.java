package com.bookshelf.controller;

import com.bookshelf.dto.recommendation.RecommendationItemDTO;
import com.bookshelf.dto.recommendation.RecommendationResponseDTO;
import com.bookshelf.dto.recommendation.UserPreferencesDTO;
import com.bookshelf.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<RecommendationResponseDTO> getRecommendations(@AuthenticationPrincipal UUID userId) {
        log.debug("Персональные рекомендации: userId={}", userId);
        RecommendationResponseDTO result = recommendationService.getPersonalRecommendations(userId);
        log.debug("Рекомендации сформированы: userId={}", userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/similar/{bookId}")
    public ResponseEntity<List<RecommendationItemDTO>> getSimilarBooks(@PathVariable UUID bookId) {
        log.debug("Похожие книги: bookId={}", bookId);
        return ResponseEntity.ok(recommendationService.getSimilarBooks(bookId));
    }

    @GetMapping("/preferences")
    public ResponseEntity<UserPreferencesDTO> getPreferences(@AuthenticationPrincipal UUID userId) {
        log.debug("Предпочтения пользователя: userId={}", userId);
        return ResponseEntity.ok(recommendationService.getPreferences(userId));
    }

    @PutMapping("/preferences")
    public ResponseEntity<UserPreferencesDTO> updatePreferences(
            @AuthenticationPrincipal UUID userId,
            @RequestBody UserPreferencesDTO dto) {
        log.info("Обновление предпочтений: userId={}", userId);
        return ResponseEntity.ok(recommendationService.updatePreferences(userId, dto));
    }
}
