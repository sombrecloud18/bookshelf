package com.bookshelf.service;

import com.bookshelf.exception.AppException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Persists user-uploaded images (avatars, book covers) to a configurable directory and
 * returns a public URL that points at the static-resource handler.
 */
@Service
@Slf4j
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final List<String> SCOPES = List.of("avatars", "covers");

    @Value("${file.upload-dir:${user.dir}/uploads}")
    private String uploadDir;

    @PostConstruct
    void init() {
        try {
            for (String scope : SCOPES) {
                Path dir = Paths.get(uploadDir, scope).toAbsolutePath().normalize();
                Files.createDirectories(dir);
            }
            log.info("Хранилище файлов инициализировано: {}", uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать директорию для загрузок: " + uploadDir, e);
        }
    }

    /** Stores the file under {scope}/{uuid}.{ext} and returns the public URL. */
    public String store(MultipartFile file, String scope) {
        if (file == null || file.isEmpty()) {
            throw AppException.badRequest("Файл пустой");
        }
        if (file.getSize() > MAX_BYTES) {
            throw AppException.badRequest("Файл слишком большой (максимум 5 МБ)");
        }
        if (!SCOPES.contains(scope)) {
            throw AppException.badRequest("Неизвестная категория загрузки: " + scope);
        }

        String ext = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTS.contains(ext)) {
            throw AppException.badRequest("Недопустимый формат файла. Разрешены: " + String.join(", ", ALLOWED_EXTS));
        }

        String filename = UUID.randomUUID() + "." + ext;
        Path target = Paths.get(uploadDir, scope, filename).toAbsolutePath().normalize();

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Не удалось сохранить файл: {}", target, e);
            throw new IllegalStateException("Не удалось сохранить файл", e);
        }

        String publicUrl = "/files/" + scope + "/" + filename;
        log.info("Файл загружен: scope={}, url={}", scope, publicUrl);
        return publicUrl;
    }

    public String getRootDir() {
        return uploadDir;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) return "";
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) return "";
        return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
