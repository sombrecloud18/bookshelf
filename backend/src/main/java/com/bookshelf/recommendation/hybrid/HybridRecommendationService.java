package com.bookshelf.recommendation.hybrid;

import com.bookshelf.config.RecommendationProperties;
import com.bookshelf.recommendation.cb.ContentBasedService;
import com.bookshelf.recommendation.cf.CollaborativeFilteringService;
import com.bookshelf.recommendation.cf.InteractionMatrixBuilder;
import com.bookshelf.recommendation.cf.ScoredBook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Weighted hybrid: blends collaborative-filtering and content-based scores
 * with adaptive λ. The blending weight grows with the user's interaction
 * count, since CF is reliable only when we have enough history.
 *
 * <pre>
 *   λ = lambdaCold  if interactionCount < coldThreshold
 *   λ = lambdaWarm  if interactionCount in [coldThreshold, warmThreshold]
 *   λ = lambdaHot   otherwise
 *
 *   final(b) = λ · norm(score_CF(b)) + (1 - λ) · norm(score_CB(b))
 * </pre>
 *
 * Scores from each algorithm are min-max normalised to [0..1] before
 * blending so they're directly comparable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HybridRecommendationService {

    private final CollaborativeFilteringService cf;
    private final ContentBasedService cb;
    private final InteractionMatrixBuilder matrixBuilder;
    private final RecommendationProperties props;
    private final JdbcTemplate jdbc;

    public enum Source {
        CF, CB, HYBRID, POPULAR, NEW, RANDOM
    }

    public record HybridResult(UUID bookId, double score, Source source) {}

    @Transactional(readOnly = true)
    public List<HybridResult> recommend(UUID userId, int topN) {
        int interactionCount = matrixBuilder.build().ratingsByUser(userId).size();
        double lambda = pickLambda(interactionCount);
        log.debug("Hybrid: userId={}, interactionCount={}, lambda={}", userId, interactionCount, lambda);

        // Pull a generous pool from each, blend, then take top-N
        int poolSize = Math.max(topN * 4, 30);
        List<ScoredBook> cfList = lambda > 0 ? cf.scoreForUser(userId, poolSize) : Collections.emptyList();
        List<ScoredBook> cbList = lambda < 1.0 ? cb.scoreForUser(userId, poolSize) : Collections.emptyList();

        Map<UUID, Double> cfNorm = minMaxNormalise(cfList);
        Map<UUID, Double> cbNorm = minMaxNormalise(cbList);

        // Determine per-book source
        Map<UUID, Source> sourceMap = new HashMap<>();
        Map<UUID, Double> blended = new HashMap<>();
        for (UUID id : union(cfNorm.keySet(), cbNorm.keySet())) {
            double s = lambda * cfNorm.getOrDefault(id, 0.0)
                    + (1.0 - lambda) * cbNorm.getOrDefault(id, 0.0);
            blended.put(id, s);
            boolean inCf = cfNorm.containsKey(id);
            boolean inCb = cbNorm.containsKey(id);
            Source src;
            if (inCf && inCb) src = Source.HYBRID;
            else if (inCf) src = Source.CF;
            else src = Source.CB;
            sourceMap.put(id, src);
        }

        List<HybridResult> out = new ArrayList<>(blended.size());
        for (Map.Entry<UUID, Double> e : blended.entrySet()) {
            out.add(new HybridResult(e.getKey(), e.getValue(), sourceMap.get(e.getKey())));
        }
        out.sort((a, b) -> Double.compare(b.score(), a.score()));
        return out.size() > topN ? out.subList(0, topN) : out;
    }

    /**
     * Persists every served recommendation to {@code recommendation_log}
     * for analytics. Called by the public-facing service after slicing
     * the final list; runs in a fresh transaction (no failure propagates).
     */
    public void logServed(UUID userId, List<HybridResult> results) {
        if (results.isEmpty()) return;
        List<Object[]> batch = new ArrayList<>(results.size());
        Timestamp now = Timestamp.from(Instant.now());
        for (HybridResult r : results) {
            batch.add(new Object[]{
                    UUID.randomUUID(), userId, r.bookId(), r.source().name(), r.score(), now, false
            });
        }
        try {
            jdbc.batchUpdate(
                    "INSERT INTO recommendation_log (id, user_id, book_id, source, score, shown_at, clicked) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    batch);
        } catch (Exception e) {
            log.warn("Не удалось записать recommendation_log: {}", e.getMessage());
        }
    }

    private double pickLambda(int count) {
        if (count < props.getHybrid().getColdThreshold()) return props.getHybrid().getLambdaCold();
        if (count <= props.getHybrid().getWarmThreshold()) return props.getHybrid().getLambdaWarm();
        return props.getHybrid().getLambdaHot();
    }

    private static Map<UUID, Double> minMaxNormalise(List<ScoredBook> list) {
        if (list.isEmpty()) return Collections.emptyMap();
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (ScoredBook s : list) {
            if (s.score() < min) min = s.score();
            if (s.score() > max) max = s.score();
        }
        double range = max - min;
        Map<UUID, Double> out = new HashMap<>();
        for (ScoredBook s : list) {
            double v = range > 0 ? (s.score() - min) / range : 1.0;
            out.put(s.bookId(), v);
        }
        return out;
    }

    private static java.util.Set<UUID> union(java.util.Set<UUID> a, java.util.Set<UUID> b) {
        java.util.Set<UUID> s = new java.util.HashSet<>(a);
        s.addAll(b);
        return s;
    }
}
