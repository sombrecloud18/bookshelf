package com.bookshelf.service;

import com.bookshelf.dto.collection.CollectionDTO;
import com.bookshelf.dto.collection.CreateCollectionDTO;
import com.bookshelf.entity.*;
import com.bookshelf.exception.AppException;
import com.bookshelf.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    /** Statuses where the author may still edit / delete the collection.
     *  APPROVED is included: editing a published collection sends it back to PENDING for re-moderation. */
    private static final Set<String> EDITABLE_STATUSES = Set.of(STATUS_DRAFT, STATUS_REJECTED, STATUS_APPROVED);

    private final CollectionRepository collectionRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final LikeService likeService;

    @Transactional
    public CollectionDTO createCollection(UUID userId, CreateCollectionDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("Пользователь не найден"));

        Collection collection = Collection.builder()
                .user(user)
                .title(dto.getTitle())
                .genre(dto.getGenre())
                .description(dto.getDescription())
                .status(STATUS_DRAFT)
                .build();

        collection = collectionRepository.save(collection);
        attachBooks(collection, dto.getBookIds());
        CollectionDTO result = toDTO(collectionRepository.save(collection));
        log.info("Подборка создана: id={}, userId={}, title='{}'", result.getId(), userId, result.getTitle());
        return result;
    }

    @Transactional
    public CollectionDTO updateCollection(UUID userId, UUID collectionId, CreateCollectionDTO dto) {
        Collection collection = findById(collectionId);

        if (!collection.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Вы не можете редактировать чужую подборку");
        }
        if (!EDITABLE_STATUSES.contains(collection.getStatus())) {
            throw AppException.badRequest("Подборку нельзя править после отправки на модерацию. " +
                    "Удалите её и создайте заново или дождитесь решения модератора.");
        }

        if (dto.getTitle() != null) collection.setTitle(dto.getTitle());
        if (dto.getGenre() != null) collection.setGenre(dto.getGenre());
        if (dto.getDescription() != null) collection.setDescription(dto.getDescription());

        collection.getCollectionBooks().clear();
        attachBooks(collection, dto.getBookIds());

        // Editing a rejected collection drops it back to DRAFT (author resubmits manually).
        // Editing an already-published one is treated as a new submission — straight to PENDING.
        if (STATUS_REJECTED.equals(collection.getStatus())) {
            collection.setStatus(STATUS_DRAFT);
            collection.setModeratorComment(null);
        } else if (STATUS_APPROVED.equals(collection.getStatus())) {
            collection.setStatus(STATUS_PENDING);
            collection.setModeratorComment(null);
        }

        return toDTO(collectionRepository.save(collection));
    }

    @Transactional
    public void deleteCollection(UUID userId, UUID collectionId, boolean isModerator) {
        Collection collection = findById(collectionId);

        if (!isModerator && !collection.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Вы не можете удалить чужую подборку");
        }

        // The likes table is polymorphic (no FK to collections) so we clean it up manually
        // — both the collection's own likes and likes on each of its comments.
        likeService.deleteFor(LikeService.TARGET_COLLECTION, collectionId);
        commentRepository.findByCollectionIdOrderByCreatedAtAsc(collectionId)
                .forEach(c -> likeService.deleteFor(LikeService.TARGET_COMMENT, c.getId()));

        collectionRepository.delete(collection);
    }

    @Transactional
    public CollectionDTO publishCollection(UUID userId, UUID collectionId) {
        Collection collection = findById(collectionId);

        if (!collection.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Вы не можете опубликовать чужую подборку");
        }
        if (!EDITABLE_STATUSES.contains(collection.getStatus())) {
            throw AppException.badRequest("Подборка уже находится на модерации или одобрена");
        }
        if (collection.getCollectionBooks().isEmpty()) {
            throw AppException.badRequest("В подборке должна быть хотя бы одна книга");
        }

        collection.setStatus(STATUS_PENDING);
        collection.setModeratorComment(null);
        CollectionDTO result = toDTO(collectionRepository.save(collection));
        log.info("Подборка отправлена на модерацию: id={}, userId={}", collectionId, userId);
        return result;
    }

    @Transactional(readOnly = true)
    public List<CollectionDTO> getUserCollections(UUID userId) {
        return collectionRepository.findByUserId(userId)
                .stream().map(c -> toDTO(c, userId)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CollectionDTO> getApprovedCollections(String query, Pageable pageable, UUID viewerId) {
        Page<Collection> page = (query != null && !query.isBlank())
                ? collectionRepository.searchByKeyword(query, pageable)
                : collectionRepository.findByStatus(STATUS_APPROVED, pageable);
        return page.map(c -> toDTO(c, viewerId));
    }

    @Transactional(readOnly = true)
    public Page<CollectionDTO> getPendingCollections(Pageable pageable) {
        return collectionRepository.findByStatus(STATUS_PENDING, pageable).map(this::toDTO);
    }

    @Transactional
    public CollectionDTO approveCollection(UUID collectionId, String moderatorComment) {
        Collection collection = findById(collectionId);
        collection.setStatus(STATUS_APPROVED);
        collection.setModeratorComment(moderatorComment);
        log.info("Подборка одобрена: id={}", collectionId);
        return toDTO(collectionRepository.save(collection));
    }

    @Transactional
    public CollectionDTO rejectCollection(UUID collectionId, String moderatorComment) {
        Collection collection = findById(collectionId);
        collection.setStatus(STATUS_REJECTED);
        collection.setModeratorComment(moderatorComment);
        log.info("Подборка отклонена: id={}, comment='{}'", collectionId, moderatorComment);
        return toDTO(collectionRepository.save(collection));
    }

    public Collection findById(UUID id) {
        return collectionRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Подборка не найдена"));
    }

    private void attachBooks(Collection collection, List<UUID> bookIds) {
        if (bookIds == null) return;

        for (int i = 0; i < bookIds.size(); i++) {
            final int position = i;
            UUID bookId = bookIds.get(i);
            bookRepository.findById(bookId).ifPresent(book -> {
                CollectionBook cb = CollectionBook.builder()
                        .id(new CollectionBook.CollectionBookId(collection.getId(), book.getId()))
                        .collection(collection)
                        .book(book)
                        .position(position)
                        .build();
                collection.getCollectionBooks().add(cb);
            });
        }
    }

    public CollectionDTO toDTO(Collection c) {
        return toDTO(c, null);
    }

    public CollectionDTO toDTO(Collection c, UUID viewerId) {
        List<UUID> bookIds = c.getCollectionBooks().stream()
                .map(cb -> cb.getBook().getId())
                .collect(Collectors.toList());

        long likes = likeService.count(LikeService.TARGET_COLLECTION, c.getId());
        boolean liked = viewerId != null && !likeService
                .findLikedIds(viewerId, LikeService.TARGET_COLLECTION, List.of(c.getId())).isEmpty();

        return CollectionDTO.builder()
                .id(c.getId())
                .userId(c.getUser().getId())
                .title(c.getTitle())
                .genre(c.getGenre())
                .description(c.getDescription())
                .status(c.getStatus())
                .moderatorComment(c.getModeratorComment())
                .bookIds(bookIds)
                .author(c.getUser().getLogin())
                .authorName(buildFullName(c.getUser()))
                .createdAt(c.getCreatedAt())
                .likes(likes)
                .liked(liked)
                .build();
    }

    private String buildFullName(User user) {
        StringBuilder sb = new StringBuilder();
        if (user.getLastName() != null) sb.append(user.getLastName()).append(" ");
        if (user.getFirstName() != null) sb.append(user.getFirstName());
        return sb.toString().trim();
    }
}
