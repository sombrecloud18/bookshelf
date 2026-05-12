package com.bookshelf.recommendation.cb;

import com.bookshelf.config.RecommendationProperties;
import com.bookshelf.entity.Book;
import com.bookshelf.entity.UserPreference;
import com.bookshelf.recommendation.cf.InteractionMatrix;
import com.bookshelf.recommendation.cf.InteractionMatrixBuilder;
import com.bookshelf.recommendation.cf.ScoredBook;
import com.bookshelf.repository.BookRepository;
import com.bookshelf.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Content-based filtering. Builds a user profile vector as a weighted sum of
 * TF-IDF vectors of books the user reacted positively to, then scores
 * candidate books via cosine similarity. Adds structured bonuses for
 * declared favourite genres / authors (from {@code user_preferences}).
 *
 * <pre>
 *   score(u, b) = α · cosine(profile(u), vec(b))
 *               + β · I[b.genre ∈ favouriteGenres(u)]
 *               + γ · I[b.author contains favouriteAuthor of u]
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentBasedService {

    private final BookVectorService bookVectorService;
    private final BookRepository bookRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final InteractionMatrixBuilder matrixBuilder;
    private final RecommendationProperties props;

    @Transactional(readOnly = true)
    public List<ScoredBook> scoreForUser(UUID userId, int topN) {
        Map<UUID, Map<String, Double>> bookVecs = bookVectorService.loadAllVectors();
        if (bookVecs.isEmpty()) {
            log.warn("book_tfidf пуста — нечего рекомендовать через CB");
            return Collections.emptyList();
        }

        InteractionMatrix matrix = matrixBuilder.build();
        Map<UUID, Double> ratings = matrix.ratingsByUser(userId);

        Optional<UserPreference> prefOpt = preferenceRepository.findByUserId(userId);
        Set<String> favGenres = prefOpt
                .filter(p -> p.getFavoriteGenres() != null)
                .map(p -> new HashSet<>(java.util.Arrays.asList(p.getFavoriteGenres())))
                .orElse(new HashSet<>());
        Set<String> favAuthors = prefOpt
                .filter(p -> p.getFavoriteAuthors() != null)
                .map(p -> new HashSet<>(java.util.Arrays.asList(p.getFavoriteAuthors())))
                .orElse(new HashSet<>());

        // Build profile vector (weighted sum of liked books)
        Map<String, Double> profile = new HashMap<>();
        double weightSum = 0;
        for (Map.Entry<UUID, Double> r : ratings.entrySet()) {
            if (r.getValue() < 3.0) continue;  // only positive signals
            Map<String, Double> v = bookVecs.get(r.getKey());
            if (v == null) continue;
            TfIdfCalculator.accumulate(profile, v, r.getValue());
            weightSum += r.getValue();
        }
        if (weightSum > 0) {
            for (Map.Entry<String, Double> e : profile.entrySet()) e.setValue(e.getValue() / weightSum);
            TfIdfCalculator.normalise(profile);
        }

        boolean hasInteractionProfile = !profile.isEmpty();
        Set<UUID> alreadySeen = new HashSet<>(ratings.keySet());

        Map<UUID, Book> bookMap = new HashMap<>();
        for (Book b : bookRepository.findAll()) bookMap.put(b.getId(), b);

        java.util.List<ScoredBook> result = new java.util.ArrayList<>();
        for (Map.Entry<UUID, Map<String, Double>> e : bookVecs.entrySet()) {
            UUID bid = e.getKey();
            if (alreadySeen.contains(bid)) continue;

            double textSim = hasInteractionProfile
                    ? TfIdfCalculator.cosine(profile, e.getValue())
                    : 0.0;

            Book b = bookMap.get(bid);
            double genreBoost = (b != null && b.getGenre() != null && favGenres.contains(b.getGenre())) ? 1.0 : 0.0;
            double authorBoost = 0.0;
            if (b != null && b.getAuthor() != null) {
                for (String fa : favAuthors) {
                    if (fa != null && !fa.isBlank() && b.getAuthor().contains(fa)) {
                        authorBoost = 1.0;
                        break;
                    }
                }
            }

            double score = props.getCb().getAlphaText() * textSim
                    + props.getCb().getBetaGenre() * genreBoost
                    + props.getCb().getGammaAuthor() * authorBoost;

            if (score > 0) result.add(new ScoredBook(bid, score));
        }
        Collections.sort(result);
        return result.size() > topN ? result.subList(0, topN) : result;
    }
}
