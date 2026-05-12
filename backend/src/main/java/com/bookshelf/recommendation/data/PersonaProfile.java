package com.bookshelf.recommendation.data;

import java.util.List;
import java.util.Map;

/**
 * Latent user archetype used by the synthetic data generator.
 * The pair (genreWeights, authorWeights) defines what books a user of this
 * persona is statistically likely to interact with. Weights are unnormalised;
 * the generator computes a softmax-like distribution across the catalog.
 *
 * @param name           short identifier ("humanitarian", "programmer", ...)
 * @param displayName    human-readable label for logs / reports
 * @param genreWeights   map of book.genre → affinity weight (typical: 0.0–1.0)
 * @param authorWeights  map of substring of book.author → bonus weight
 * @param favoriteGenres genres copied into user_preferences for cold-start fallback
 * @param favoriteAuthors author tokens copied into user_preferences
 */
public record PersonaProfile(
        String name,
        String displayName,
        Map<String, Double> genreWeights,
        Map<String, Double> authorWeights,
        List<String> favoriteGenres,
        List<String> favoriteAuthors
) {
}
