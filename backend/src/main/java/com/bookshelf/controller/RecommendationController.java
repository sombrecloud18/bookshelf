package com.bookshelf.controller;

import com.bookshelf.dto.recommendation.RecommendationItemDTO;
import com.bookshelf.dto.recommendation.RecommendationResponseDTO;
import com.bookshelf.dto.recommendation.UserPreferencesDTO;
import com.bookshelf.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<RecommendationResponseDTO> getRecommendations(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(recommendationService.getPersonalRecommendations(userId));
    }

    @GetMapping("/similar/{bookId}")
    public ResponseEntity<List<RecommendationItemDTO>> getSimilarBooks(@PathVariable UUID bookId) {
        return ResponseEntity.ok(recommendationService.getSimilarBooks(bookId));
    }

    @GetMapping("/preferences")
    public ResponseEntity<UserPreferencesDTO> getPreferences(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(recommendationService.getPreferences(userId));
    }

    @PutMapping("/preferences")
    public ResponseEntity<UserPreferencesDTO> updatePreferences(
            @AuthenticationPrincipal UUID userId,
            @RequestBody UserPreferencesDTO dto) {
        return ResponseEntity.ok(recommendationService.updatePreferences(userId, dto));
    }
}
