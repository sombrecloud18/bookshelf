package com.bookshelf.service;

import com.bookshelf.dto.recommendation.RecommendationItemDTO;
import com.bookshelf.dto.recommendation.RecommendationResponseDTO;
import com.bookshelf.dto.recommendation.UserPreferencesDTO;
import com.bookshelf.entity.Book;
import com.bookshelf.entity.User;
import com.bookshelf.entity.UserPreference;
import com.bookshelf.exception.AppException;
import com.bookshelf.recommendation.cf.CollaborativeFilteringService;
import com.bookshelf.recommendation.cf.ScoredBook;
import com.bookshelf.recommendation.hybrid.HybridRecommendationService;
import com.bookshelf.recommendation.hybrid.HybridRecommendationService.HybridResult;
import com.bookshelf.recommendation.hybrid.HybridRecommendationService.Source;
import com.bookshelf.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Public-facing recommendation orchestrator. Delegates the heavy lifting to
 * the {@link HybridRecommendationService} (CF + CB blend) and packages the
 * result alongside legacy "popular" and "new books" carousels into the
 * shape expected by the frontend ({@link RecommendationResponseDTO}).
 *
 * For anonymous viewers (no userId) the personal carousel is skipped — only
 * popular & new are returned, which the existing widget handles gracefully.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final BookRepository bookRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final HybridRecommendationService hybrid;
    private final CollaborativeFilteringService cf;

    private static final int PERSONAL_TOP_N = 10;
    private static final int POPULAR_TOP_N  = 10;
    private static final int NEW_TOP_N      = 10;

    @Transactional(readOnly = true)
    public RecommendationResponseDTO getPersonalRecommendations(UUID userId) {
        log.debug("Формирование рекомендаций: userId={}", userId);

        // POPULAR: top-rated approved books, always available
        List<RecommendationItemDTO> popular = bookRepository
                .findTopRatedBooks(POPULAR_TOP_N).stream()
                .map(b -> toItem(b, Source.POPULAR.name(), null))
                .collect(Collectors.toList());

        // NEW: most recently added active books
        List<RecommendationItemDTO> newBooks = bookRepository
                .findAll(PageRequest.of(0, NEW_TOP_N, Sort.by("createdAt").descending())).stream()
                .map(b -> toItem(b, Source.NEW.name(), null))
                .collect(Collectors.toList());

        // PERSONAL: hybrid CF+CB, or random unread fallback for anonymous users
        List<RecommendationItemDTO> personal;
        if (userId == null) {
            personal = popular;  // anonymous viewers see "popular" in the personal slot
        } else {
            List<HybridResult> hybridResults = hybrid.recommend(userId, PERSONAL_TOP_N);
            if (hybridResults.isEmpty()) {
                // Cold start without any preferences: fall back to popular
                personal = popular;
            } else {
                List<UUID> ids = hybridResults.stream().map(HybridResult::bookId).collect(Collectors.toList());
                Map<UUID, Book> bookMap = bookRepository.findByIdIn(ids).stream()
                        .collect(Collectors.toMap(Book::getId, b -> b));
                personal = hybridResults.stream()
                        .map(hr -> {
                            Book b = bookMap.get(hr.bookId());
                            return b == null ? null : toItem(b, hr.source().name(), hr.score());
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());
                hybrid.logServed(userId, hybridResults);
            }
        }

        return RecommendationResponseDTO.builder()
                .personal(personal)
                .popular(popular)
                .newBooks(newBooks)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RecommendationItemDTO> getSimilarBooks(UUID bookId) {
        Book seed = bookRepository.findById(bookId).orElse(null);
        if (seed == null) return List.of();

        List<ScoredBook> similar = cf.similarTo(bookId, 6);
        if (!similar.isEmpty()) {
            List<UUID> ids = similar.stream().map(ScoredBook::bookId).collect(Collectors.toList());
            Map<UUID, Book> map = new HashMap<>();
            for (Book b : bookRepository.findByIdIn(ids)) map.put(b.getId(), b);
            return similar.stream()
                    .map(s -> {
                        Book b = map.get(s.bookId());
                        return b == null ? null : toItem(b, "SIMILAR_CF", s.score());
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // Fallback: same-genre books when CF matrix has no neighbours for this book
        return bookRepository.findSimilarByGenre(seed.getGenre(), bookId, PageRequest.of(0, 6))
                .stream()
                .map(b -> toItem(b, "SIMILAR_GENRE", null))
                .collect(Collectors.toList());
    }

    @Transactional
    public UserPreferencesDTO updatePreferences(UUID userId, UserPreferencesDTO dto) {
        Optional<UserPreference> existing = userPreferenceRepository.findByUserId(userId);

        UserPreference pref;
        if (existing.isPresent()) {
            pref = existing.get();
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> AppException.notFound("Пользователь не найден"));
            pref = new UserPreference();
            pref.setUser(user);
        }

        if (dto.getFavoriteGenres() != null) {
            pref.setFavoriteGenres(dto.getFavoriteGenres().toArray(new String[0]));
        }
        if (dto.getFavoriteAuthors() != null) {
            pref.setFavoriteAuthors(dto.getFavoriteAuthors().toArray(new String[0]));
        }

        userPreferenceRepository.save(pref);
        return toPreferencesDTO(pref);
    }

    @Transactional(readOnly = true)
    public UserPreferencesDTO getPreferences(UUID userId) {
        return userPreferenceRepository.findByUserId(userId)
                .map(this::toPreferencesDTO)
                .orElse(UserPreferencesDTO.builder()
                        .favoriteGenres(List.of())
                        .favoriteAuthors(List.of())
                        .readingHistory(List.of())
                        .build());
    }

    private RecommendationItemDTO toItem(Book b, String source, Double matchScore) {
        Double rating = reviewRepository.getAverageRatingByBookId(b.getId());
        return RecommendationItemDTO.builder()
                .id(b.getId())
                .bookId(b.getId())
                .title(b.getTitle())
                .genre(b.getGenre())
                .author(b.getAuthor())
                .imageUrl(b.getImageUrl())
                .description(b.getDescription())
                .matchScore(matchScore != null ? matchScore : (rating != null ? rating / 5.0 : 0.0))
                .source(source)
                .views(0L)
                .addedDate(LocalDate.now().toString())
                .build();
    }

    private UserPreferencesDTO toPreferencesDTO(UserPreference pref) {
        return UserPreferencesDTO.builder()
                .favoriteGenres(pref.getFavoriteGenres() != null
                        ? Arrays.asList(pref.getFavoriteGenres()) : List.of())
                .favoriteAuthors(pref.getFavoriteAuthors() != null
                        ? Arrays.asList(pref.getFavoriteAuthors()) : List.of())
                .readingHistory(List.of())
                .build();
    }
}
