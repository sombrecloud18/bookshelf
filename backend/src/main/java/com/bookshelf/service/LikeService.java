package com.bookshelf.service;

import com.bookshelf.dto.like.LikeStateDTO;
import com.bookshelf.entity.Like;
import com.bookshelf.exception.AppException;
import com.bookshelf.repository.CollectionRepository;
import com.bookshelf.repository.CommentRepository;
import com.bookshelf.repository.LikeRepository;
import com.bookshelf.repository.ReviewRepository;
import com.bookshelf.repository.SubjectCollectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeService {

    public static final String TARGET_REVIEW = "REVIEW";
    public static final String TARGET_COMMENT = "COMMENT";
    public static final String TARGET_COLLECTION = "COLLECTION";
    public static final String TARGET_SUBJECT_COLLECTION = "SUBJECT_COLLECTION";

    private static final Set<String> ALLOWED_TYPES =
            Set.of(TARGET_REVIEW, TARGET_COMMENT, TARGET_COLLECTION, TARGET_SUBJECT_COLLECTION);

    private final LikeRepository likeRepository;
    private final ReviewRepository reviewRepository;
    private final CommentRepository commentRepository;
    private final CollectionRepository collectionRepository;
    private final SubjectCollectionRepository subjectCollectionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public LikeStateDTO toggle(UUID userId, String targetType, UUID targetId) {
        String type = normalizeType(targetType);
        ensureTargetExists(type, targetId);

        var existing = likeRepository.findByUserIdAndTargetTypeAndTargetId(userId, type, targetId);
        boolean liked;
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            liked = false;
        } else {
            likeRepository.save(Like.builder()
                    .userId(userId)
                    .targetType(type)
                    .targetId(targetId)
                    .build());
            liked = true;
        }

        long count = likeRepository.countByTargetTypeAndTargetId(type, targetId);
        log.debug("Лайк обновлён: userId={}, type={}, targetId={}, liked={}, count={}",
                userId, type, targetId, liked, count);

        // Broadcast a non-personalised snapshot — clients merge it with their own `liked` state.
        LikeStateDTO broadcast = LikeStateDTO.builder()
                .targetType(type)
                .targetId(targetId)
                .count(count)
                .liked(null)
                .build();
        messagingTemplate.convertAndSend("/topic/likes/" + type.toLowerCase() + "/" + targetId, broadcast);

        return LikeStateDTO.builder()
                .targetType(type)
                .targetId(targetId)
                .count(count)
                .liked(liked)
                .build();
    }

    @Transactional(readOnly = true)
    public LikeStateDTO getState(UUID userId, String targetType, UUID targetId) {
        String type = normalizeType(targetType);
        long count = likeRepository.countByTargetTypeAndTargetId(type, targetId);
        boolean liked = userId != null
                && likeRepository.findByUserIdAndTargetTypeAndTargetId(userId, type, targetId).isPresent();
        return LikeStateDTO.builder()
                .targetType(type)
                .targetId(targetId)
                .count(count)
                .liked(liked)
                .build();
    }

    @Transactional(readOnly = true)
    public long count(String targetType, UUID targetId) {
        return likeRepository.countByTargetTypeAndTargetId(normalizeType(targetType), targetId);
    }

    @Transactional(readOnly = true)
    public Set<UUID> findLikedIds(UUID userId, String targetType, Collection<UUID> ids) {
        if (userId == null || ids == null || ids.isEmpty()) return Collections.emptySet();
        return new HashSet<>(likeRepository.findLikedTargetIds(userId, normalizeType(targetType), ids));
    }

    /** Cleans up likes when the target is removed (no FK in the polymorphic table). */
    @Transactional
    public void deleteFor(String targetType, UUID targetId) {
        likeRepository.deleteAllByTarget(normalizeType(targetType), targetId);
    }

    private String normalizeType(String targetType) {
        if (targetType == null) throw AppException.badRequest("targetType обязателен");
        // Accept the dashed variant from URL paths ("subject-collection") for ergonomics.
        String upper = targetType.trim().toUpperCase().replace('-', '_');
        if (!ALLOWED_TYPES.contains(upper)) {
            throw AppException.badRequest("Недопустимый тип лайка: " + targetType);
        }
        return upper;
    }

    private void ensureTargetExists(String type, UUID targetId) {
        boolean exists = switch (type) {
            case TARGET_REVIEW -> reviewRepository.existsById(targetId);
            case TARGET_COMMENT -> commentRepository.existsById(targetId);
            case TARGET_COLLECTION -> collectionRepository.existsById(targetId);
            case TARGET_SUBJECT_COLLECTION -> subjectCollectionRepository.existsById(targetId);
            default -> false;
        };
        if (!exists) {
            throw AppException.notFound("Цель лайка не найдена");
        }
    }
}
