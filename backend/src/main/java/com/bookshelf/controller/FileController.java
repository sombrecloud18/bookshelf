package com.bookshelf.controller;

import com.bookshelf.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileStorageService fileStorageService;

    /** Authenticated users may upload their own avatar. */
    @PostMapping(value = "/avatars", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(file, "avatars");
        return ResponseEntity.ok(Map.of("url", url));
    }

    /** Only moderators may upload book covers — keep upload surface narrow. */
    @PostMapping(value = "/covers", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Map<String, String>> uploadCover(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(file, "covers");
        return ResponseEntity.ok(Map.of("url", url));
    }
}
