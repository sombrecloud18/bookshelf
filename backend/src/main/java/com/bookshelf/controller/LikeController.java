package com.bookshelf.controller;

import com.bookshelf.dto.like.LikeStateDTO;
import com.bookshelf.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
@Slf4j
public class LikeController {

    private final LikeService likeService;

    /** Toggle a like on a target. Returns new like state for the requester. */
    @PostMapping("/{targetType}/{targetId}/toggle")
    public ResponseEntity<LikeStateDTO> toggle(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String targetType,
            @PathVariable UUID targetId) {
        log.debug("Toggle like: userId={}, type={}, targetId={}", userId, targetType, targetId);
        return ResponseEntity.ok(likeService.toggle(userId, targetType, targetId));
    }

    /** Returns the like state for a target. */
    @GetMapping("/{targetType}/{targetId}")
    public ResponseEntity<LikeStateDTO> getState(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String targetType,
            @PathVariable UUID targetId) {
        return ResponseEntity.ok(likeService.getState(userId, targetType, targetId));
    }
}
