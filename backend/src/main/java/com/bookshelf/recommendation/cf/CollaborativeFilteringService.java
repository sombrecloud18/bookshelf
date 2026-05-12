package com.bookshelf.recommendation.cf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Item-based collaborative filtering: predicts a user's score for a book by
 * weighting that book's similarity to books the user has already rated.
 *
 * <pre>
 *   score(u, b) = Σ_{b' ∈ rated(u)}  sim(b, b') · rᵤ,b'
 * </pre>
 *
 * Excludes books the user has already interacted with. Reads similarity
 * matrix from {@code book_similarity} (precomputed by
 * {@link CosineItemSimilarityService}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollaborativeFilteringService {

    private final JdbcTemplate jdbc;
    private final InteractionMatrixBuilder matrixBuilder;

    @Transactional(readOnly = true)
    public List<ScoredBook> scoreForUser(UUID userId, int topN) {
        InteractionMatrix matrix = matrixBuilder.build();
        Map<UUID, Double> ratings = matrix.ratingsByUser(userId);
        if (ratings.isEmpty()) return Collections.emptyList();

        Set<UUID> alreadySeen = new HashSet<>(ratings.keySet());

        // Fetch similarity neighbors for every book the user has rated
        // book_id_2 = candidate, similarity weighted by rating
        Map<UUID, Double> candidateScores = new HashMap<>();
        for (Map.Entry<UUID, Double> r : ratings.entrySet()) {
            UUID seedBook = r.getKey();
            double rating = r.getValue();

            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT book_id_2, similarity FROM book_similarity WHERE book_id_1 = ?",
                    seedBook);
            for (Map<String, Object> row : rows) {
                UUID cand = (UUID) row.get("book_id_2");
                if (alreadySeen.contains(cand)) continue;
                double sim = ((Number) row.get("similarity")).doubleValue();
                candidateScores.merge(cand, sim * rating, Double::sum);
            }
        }

        List<ScoredBook> out = new ArrayList<>(candidateScores.size());
        for (Map.Entry<UUID, Double> e : candidateScores.entrySet()) {
            out.add(new ScoredBook(e.getKey(), e.getValue()));
        }
        Collections.sort(out);  // descending by score
        return out.size() > topN ? out.subList(0, topN) : out;
    }

    /**
     * Returns top-K most similar books to a given book — for the public
     * {@code /api/recommendations/similar/{bookId}} endpoint. Reads directly
     * from {@code book_similarity}.
     */
    @Transactional(readOnly = true)
    public List<ScoredBook> similarTo(UUID bookId, int topN) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT book_id_2, similarity FROM book_similarity " +
                        "WHERE book_id_1 = ? ORDER BY similarity DESC LIMIT ?",
                bookId, topN);
        List<ScoredBook> out = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            out.add(new ScoredBook((UUID) row.get("book_id_2"),
                    ((Number) row.get("similarity")).doubleValue()));
        }
        return out;
    }
}
