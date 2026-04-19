package com.bookshelf.service;

import com.bookshelf.dto.recommendation.RecommendationItemDTO;
import com.bookshelf.dto.recommendation.RecommendationResponseDTO;
import com.bookshelf.dto.recommendation.UserPreferencesDTO;
import com.bookshelf.entity.Book;
import com.bookshelf.entity.User;
import com.bookshelf.entity.UserPreference;
import com.bookshelf.exception.AppException;
import com.bookshelf.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final BookRepository bookRepository;
    private final UserActivityRepository userActivityRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public RecommendationResponseDTO getPersonalRecommendations(UUID userId) {
        // Popular books (top rated)
        List<RecommendationItemDTO> popular = bookRepository
                .findTopRatedBooks(10)
                .stream()
                .map(b -> toItem(b, "popular"))
                .collect(Collectors.toList());

        // Collaborative filtering: returns List<Object[]> with [book_id, count]
        List<Object[]> collaborative = userActivityRepository
                .findCollaborativeRecommendations(userId.toString(), 10);

        List<RecommendationItemDTO> personal;
        if (!collaborative.isEmpty()) {
            List<UUID> bookIds = collaborative.stream()
                    .map(row -> UUID.fromString(row[0].toString()))
                    .collect(Collectors.toList());
            personal = bookRepository.findByIdIn(bookIds).stream()
                    .map(b -> toItem(b, "collaborative"))
                    .collect(Collectors.toList());
        } else {
            // Fallback: random unread books
            personal = bookRepository
                    .findRandomUnreadBooks(userId.toString(), 10)
                    .stream()
                    .map(b -> toItem(b, "random"))
                    .collect(Collectors.toList());
        }

        // New books (recently added)
        List<RecommendationItemDTO> newBooks = bookRepository
                .findAll(PageRequest.of(0, 10, Sort.by("createdAt").descending()))
                .stream()
                .map(b -> toItem(b, "new"))
                .collect(Collectors.toList());

        return RecommendationResponseDTO.builder()
                .personal(personal)
                .popular(popular)
                .newBooks(newBooks)
                .build();
    }

    public List<RecommendationItemDTO> getSimilarBooks(UUID bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) return List.of();

        return bookRepository.findSimilarByGenre(book.getGenre(), bookId, PageRequest.of(0, 6))
                .stream()
                .map(b -> toItem(b, "similar"))
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

    public UserPreferencesDTO getPreferences(UUID userId) {
        return userPreferenceRepository.findByUserId(userId)
                .map(this::toPreferencesDTO)
                .orElse(UserPreferencesDTO.builder()
                        .favoriteGenres(List.of())
                        .favoriteAuthors(List.of())
                        .readingHistory(List.of())
                        .build());
    }

    private RecommendationItemDTO toItem(Book b, String type) {
        Double avgRating = reviewRepository.getAverageRatingByBookId(b.getId());
        return RecommendationItemDTO.builder()
                .id(b.getId())
                .bookId(b.getId())
                .title(b.getTitle())
                .genre(b.getGenre())
                .author(b.getAuthor())
                .imageUrl(b.getImageUrl())
                .description(b.getDescription())
                .matchScore(avgRating != null ? avgRating : 0.0)
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
