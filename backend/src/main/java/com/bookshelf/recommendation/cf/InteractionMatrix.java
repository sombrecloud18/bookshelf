package com.bookshelf.recommendation.cf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sparse user × item rating matrix used as input to collaborative filtering.
 *
 * Ratings are derived by aggregating multiple signal types (review,
 * collection, order, view) for each (user, book) pair — see
 * {@link InteractionMatrixBuilder}. Storage layout is two-level
 * {@code Map<user, Map<book, score>>} which is convenient both for per-user
 * lookup (CF scoring) and for per-item iteration (similarity computation,
 * via the inverted index built on demand).
 */
public class InteractionMatrix {

    private final Map<UUID, Map<UUID, Double>> userIndex = new HashMap<>();

    public void put(UUID userId, UUID bookId, double rating) {
        userIndex.computeIfAbsent(userId, u -> new HashMap<>())
                .merge(bookId, rating, Math::max);
    }

    public Set<UUID> users() {
        return userIndex.keySet();
    }

    public Map<UUID, Double> ratingsByUser(UUID userId) {
        return userIndex.getOrDefault(userId, Map.of());
    }

    /**
     * Builds an inverted index book → (user → rating). Used for cosine
     * similarity computation: every book has a sparse vector indexed by user.
     */
    public Map<UUID, Map<UUID, Double>> invertedIndex() {
        Map<UUID, Map<UUID, Double>> bookIdx = new HashMap<>();
        for (Map.Entry<UUID, Map<UUID, Double>> e : userIndex.entrySet()) {
            UUID userId = e.getKey();
            for (Map.Entry<UUID, Double> r : e.getValue().entrySet()) {
                bookIdx.computeIfAbsent(r.getKey(), b -> new HashMap<>())
                        .put(userId, r.getValue());
            }
        }
        return bookIdx;
    }

    public Set<UUID> allBooks() {
        Set<UUID> bookIds = new HashSet<>();
        for (Map<UUID, Double> row : userIndex.values()) bookIds.addAll(row.keySet());
        return bookIds;
    }

    public int totalNonZero() {
        int n = 0;
        for (Map<UUID, Double> row : userIndex.values()) n += row.size();
        return n;
    }
}
