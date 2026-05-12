package com.bookshelf.recommendation.eval;

import com.bookshelf.config.RecommendationProperties;
import com.bookshelf.entity.Book;
import com.bookshelf.recommendation.cb.BookVectorService;
import com.bookshelf.recommendation.cb.ContentBasedService;
import com.bookshelf.recommendation.cb.TfIdfCalculator;
import com.bookshelf.recommendation.cf.CollaborativeFilteringService;
import com.bookshelf.recommendation.cf.CosineItemSimilarityService;
import com.bookshelf.recommendation.cf.InteractionMatrixBuilder;
import com.bookshelf.recommendation.cf.ScoredBook;
import com.bookshelf.recommendation.hybrid.HybridRecommendationService;
import com.bookshelf.recommendation.hybrid.HybridRecommendationService.HybridResult;
import com.bookshelf.repository.BookRepository;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Offline evaluation harness for the recommendation engine.
 *
 * Methodology: persona-based ground truth + leave-out evaluation.
 *
 * <ol>
 *   <li>Take all positive signals (rating ≥ 4 OR collection-add OR order) per user</li>
 *   <li>Random 80/20 split into train/test (per-user)</li>
 *   <li>Snapshot DB, hide test interactions, rebuild book_similarity + book_tfidf
 *       on the training subset only</li>
 *   <li>For each algorithm, generate top-K predictions per user and compare
 *       against the held-out test set</li>
 *   <li>Restore DB to original state</li>
 * </ol>
 *
 * Reported metrics per algorithm:
 * <ul>
 *   <li>Precision@K = |hits ∩ test| / K — share of recommendations that the
 *       user actually engaged with in the held-out set</li>
 *   <li>Recall@K    = |hits ∩ test| / |test| — share of held-out items
 *       successfully retrieved</li>
 *   <li>NDCG@K      — discounts hits by rank</li>
 *   <li>Coverage    — fraction of catalog books reached over the test users</li>
 *   <li>Diversity   — average pairwise content-cosine within a single
 *       recommendation list (lower = more similar within list)</li>
 * </ul>
 *
 * NOTE: Because rebuilding the similarity matrix from a partial dataset is
 * an expensive serial mutation of shared tables, we serialise this method
 * via a single class-level lock. Do not call concurrently.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationService {

    private final JdbcTemplate jdbc;
    private final BookRepository bookRepository;
    private final InteractionMatrixBuilder matrixBuilder;
    private final CosineItemSimilarityService cfRebuild;
    private final CollaborativeFilteringService cfService;
    private final BookVectorService bookVectorService;
    private final ContentBasedService cbService;
    private final HybridRecommendationService hybridService;
    private final RecommendationProperties props;

    private final Object evalLock = new Object();

    @Transactional
    public EvaluationReport runFullEvaluation() {
        synchronized (evalLock) {
            return doEvaluation();
        }
    }

    private EvaluationReport doEvaluation() {
        long t0 = System.currentTimeMillis();
        UUID runId = UUID.randomUUID();
        int topK = props.getEval().getTopK();
        double trainRatio = props.getEval().getTrainRatio();
        Random rng = new Random(props.getSynthetic().getSeed() + 1);

        log.info("EVAL start: runId={}, trainRatio={}, topK={}", runId, trainRatio, topK);

        // 1. Collect positive signals per user from reviews + collection_books + orders
        Map<UUID, Set<UUID>> positives = loadPositiveSignals();
        if (positives.isEmpty()) {
            throw new IllegalStateException("Нет positive signals в БД — сначала сгенерируй синтетику");
        }

        // 2. Random per-user split
        Map<UUID, Set<UUID>> train = new HashMap<>();
        Map<UUID, Set<UUID>> test = new HashMap<>();
        int trainSize = 0, testSize = 0;
        for (Map.Entry<UUID, Set<UUID>> e : positives.entrySet()) {
            List<UUID> all = new ArrayList<>(e.getValue());
            Collections.shuffle(all, rng);
            int split = (int) Math.ceil(all.size() * trainRatio);
            Set<UUID> tr = new HashSet<>(all.subList(0, Math.min(split, all.size())));
            Set<UUID> te = new HashSet<>(all.subList(Math.min(split, all.size()), all.size()));
            if (!te.isEmpty()) {
                train.put(e.getKey(), tr);
                test.put(e.getKey(), te);
                trainSize += tr.size();
                testSize += te.size();
            }
        }
        log.info("EVAL split: users={}, train_signals={}, test_signals={}",
                train.size(), trainSize, testSize);

        // 3. Hide test signals: mark hidden reviews + collection_books + orders
        //    We do this by snapshotting affected rows then deleting them.
        Snapshot snap = hideTestData(test);

        try {
            // 4. Rebuild similarity matrices on train-only data
            cfRebuild.rebuild();
            bookVectorService.rebuildAll();

            // 5. Pre-load TF-IDF vectors for diversity computation
            Map<UUID, Map<String, Double>> bookVecs = bookVectorService.loadAllVectors();

            // 6. Evaluate algorithms
            List<EvaluationReport.AlgorithmMetrics> metrics = new ArrayList<>();
            metrics.add(evaluateAlgorithm("RANDOM",  test, runId, topK,
                    (uid) -> randomScores(uid, topK, rng), bookVecs));
            metrics.add(evaluateAlgorithm("POPULAR", test, runId, topK,
                    (uid) -> popularScores(topK), bookVecs));
            metrics.add(evaluateAlgorithm("CF",      test, runId, topK,
                    (uid) -> cfService.scoreForUser(uid, topK), bookVecs));
            metrics.add(evaluateAlgorithm("CB",      test, runId, topK,
                    (uid) -> cbService.scoreForUser(uid, topK), bookVecs));
            metrics.add(evaluateAlgorithm("HYBRID",  test, runId, topK,
                    (uid) -> hybridToScored(hybridService.recommend(uid, topK)), bookVecs));

            EvaluationReport report = EvaluationReport.builder()
                    .runId(runId)
                    .topK(topK)
                    .trainRatio(trainRatio)
                    .trainSize(trainSize)
                    .testSize(testSize)
                    .usersEvaluated(test.size())
                    .metrics(metrics)
                    .elapsedMs(System.currentTimeMillis() - t0)
                    .build();

            log.info("EVAL done in {}мс: {}", report.getElapsedMs(),
                    summary(metrics));
            return report;
        } finally {
            // 7. Restore test data + rebuild similarity to "full" state
            restore(snap);
            cfRebuild.rebuild();
            bookVectorService.rebuildAll();
            log.info("EVAL: исходное состояние БД восстановлено, матрицы пересчитаны");
        }
    }

    private List<ScoredBook> hybridToScored(List<HybridResult> rs) {
        List<ScoredBook> out = new ArrayList<>(rs.size());
        for (HybridResult r : rs) out.add(new ScoredBook(r.bookId(), r.score()));
        return out;
    }

    private EvaluationReport.AlgorithmMetrics evaluateAlgorithm(
            String name,
            Map<UUID, Set<UUID>> test,
            UUID runId,
            int K,
            java.util.function.Function<UUID, List<ScoredBook>> scorer,
            Map<UUID, Map<String, Double>> bookVecs) {

        double sumP = 0, sumR = 0, sumNdcg = 0, sumDiv = 0;
        Set<UUID> reached = new HashSet<>();
        int evaluated = 0;

        for (Map.Entry<UUID, Set<UUID>> e : test.entrySet()) {
            UUID uid = e.getKey();
            Set<UUID> truth = e.getValue();
            if (truth.isEmpty()) continue;

            List<ScoredBook> recs = scorer.apply(uid);
            if (recs.isEmpty()) continue;
            List<UUID> ranked = new ArrayList<>(recs.size());
            for (ScoredBook s : recs) ranked.add(s.bookId());
            if (ranked.size() > K) ranked = ranked.subList(0, K);
            reached.addAll(ranked);

            int hits = 0;
            double dcg = 0, idcg = 0;
            for (int i = 0; i < ranked.size(); i++) {
                if (truth.contains(ranked.get(i))) {
                    hits++;
                    dcg += 1.0 / (Math.log(i + 2) / Math.log(2));
                }
            }
            for (int i = 0; i < Math.min(K, truth.size()); i++) {
                idcg += 1.0 / (Math.log(i + 2) / Math.log(2));
            }
            double precision = (double) hits / K;
            double recall = (double) hits / truth.size();
            double ndcg = idcg > 0 ? dcg / idcg : 0;
            double diversity = computeListDiversity(ranked, bookVecs);

            sumP += precision;
            sumR += recall;
            sumNdcg += ndcg;
            sumDiv += diversity;
            evaluated++;
        }

        int totalBooks = (int) bookRepository.count();
        Map<String, Double> values = new LinkedHashMap<>();
        values.put("precision@K", evaluated == 0 ? 0 : sumP / evaluated);
        values.put("recall@K",    evaluated == 0 ? 0 : sumR / evaluated);
        values.put("ndcg@K",      evaluated == 0 ? 0 : sumNdcg / evaluated);
        values.put("coverage",    totalBooks == 0 ? 0 : (double) reached.size() / totalBooks);
        values.put("diversity",   evaluated == 0 ? 0 : sumDiv / evaluated);

        // Persist to recommendation_evaluation
        Timestamp now = Timestamp.from(Instant.now());
        for (Map.Entry<String, Double> e : values.entrySet()) {
            jdbc.update("INSERT INTO recommendation_evaluation (id, run_id, algorithm, metric_name, metric_value, config_json, computed_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    UUID.randomUUID(), runId, name, e.getKey(), e.getValue(),
                    "{\"K\":" + K + "}", now);
        }

        return EvaluationReport.AlgorithmMetrics.builder()
                .algorithm(name)
                .values(values)
                .build();
    }

    /** Mean pairwise content cosine within a single ranked list (lower = more diverse). */
    private double computeListDiversity(List<UUID> ranked, Map<UUID, Map<String, Double>> bookVecs) {
        if (ranked.size() < 2) return 0;
        double sum = 0;
        int pairs = 0;
        for (int i = 0; i < ranked.size(); i++) {
            Map<String, Double> a = bookVecs.get(ranked.get(i));
            if (a == null) continue;
            for (int j = i + 1; j < ranked.size(); j++) {
                Map<String, Double> b = bookVecs.get(ranked.get(j));
                if (b == null) continue;
                sum += TfIdfCalculator.cosine(a, b);
                pairs++;
            }
        }
        return pairs == 0 ? 0 : sum / pairs;
    }

    private List<ScoredBook> randomScores(UUID userId, int K, Random rng) {
        List<Book> all = bookRepository.findAll();
        Collections.shuffle(all, rng);
        List<ScoredBook> out = new ArrayList<>();
        for (int i = 0; i < Math.min(K, all.size()); i++) {
            out.add(new ScoredBook(all.get(i).getId(), 1.0 - (double) i / K));
        }
        return out;
    }

    private List<ScoredBook> popularScores(int K) {
        List<Book> popular = bookRepository.findTopRatedBooks(K);
        List<ScoredBook> out = new ArrayList<>(popular.size());
        for (int i = 0; i < popular.size(); i++) {
            out.add(new ScoredBook(popular.get(i).getId(), 1.0 - (double) i / K));
        }
        return out;
    }

    private Map<UUID, Set<UUID>> loadPositiveSignals() {
        Map<UUID, Set<UUID>> map = new HashMap<>();
        jdbc.query("SELECT user_id, book_id FROM reviews WHERE status='APPROVED' AND rating >= 4", rs -> {
            map.computeIfAbsent((UUID) rs.getObject("user_id"), u -> new HashSet<>())
                    .add((UUID) rs.getObject("book_id"));
        });
        jdbc.query("SELECT c.user_id AS user_id, cb.book_id AS book_id " +
                "FROM collection_books cb JOIN collections c ON c.id = cb.collection_id " +
                "WHERE c.status IN ('APPROVED','DRAFT')", rs -> {
            map.computeIfAbsent((UUID) rs.getObject("user_id"), u -> new HashSet<>())
                    .add((UUID) rs.getObject("book_id"));
        });
        jdbc.query("SELECT user_id, book_id FROM orders WHERE status='ACTIVE'", rs -> {
            map.computeIfAbsent((UUID) rs.getObject("user_id"), u -> new HashSet<>())
                    .add((UUID) rs.getObject("book_id"));
        });
        return map;
    }

    private record Snapshot(
            List<Object[]> reviews,
            List<Object[]> orders,
            List<Object[]> collectionBooks
    ) {}

    private Snapshot hideTestData(Map<UUID, Set<UUID>> test) {
        List<Object[]> reviewsSnap = new ArrayList<>();
        List<Object[]> ordersSnap = new ArrayList<>();
        List<Object[]> collectionBooksSnap = new ArrayList<>();

        for (Map.Entry<UUID, Set<UUID>> e : test.entrySet()) {
            UUID uid = e.getKey();
            for (UUID bid : e.getValue()) {
                jdbc.query("SELECT id, book_id, user_id, rating, text, status, created_at, updated_at " +
                                "FROM reviews WHERE user_id=? AND book_id=? AND status='APPROVED' AND rating>=4",
                        rs -> {
                            reviewsSnap.add(new Object[]{
                                    rs.getObject("id"), rs.getObject("book_id"), rs.getObject("user_id"),
                                    rs.getInt("rating"), rs.getString("text"), rs.getString("status"),
                                    rs.getTimestamp("created_at"), rs.getTimestamp("updated_at")
                            });
                        }, uid, bid);
                jdbc.query("SELECT id, user_id, book_id, status, created_at FROM orders WHERE user_id=? AND book_id=? AND status='ACTIVE'",
                        rs -> {
                            ordersSnap.add(new Object[]{
                                    rs.getObject("id"), rs.getObject("user_id"), rs.getObject("book_id"),
                                    rs.getString("status"), rs.getTimestamp("created_at")
                            });
                        }, uid, bid);
                jdbc.query("SELECT cb.collection_id, cb.book_id, cb.position " +
                                "FROM collection_books cb JOIN collections c ON c.id=cb.collection_id " +
                                "WHERE c.user_id=? AND cb.book_id=? AND c.status IN ('APPROVED','DRAFT')",
                        rs -> {
                            collectionBooksSnap.add(new Object[]{
                                    rs.getObject("collection_id"), rs.getObject("book_id"), rs.getInt("position")
                            });
                        }, uid, bid);

                jdbc.update("DELETE FROM reviews WHERE user_id=? AND book_id=? AND status='APPROVED' AND rating>=4",
                        uid, bid);
                jdbc.update("DELETE FROM orders WHERE user_id=? AND book_id=? AND status='ACTIVE'",
                        uid, bid);
                jdbc.update("DELETE FROM collection_books WHERE collection_id IN " +
                                "(SELECT id FROM collections WHERE user_id=? AND status IN ('APPROVED','DRAFT')) " +
                                "AND book_id=?",
                        uid, bid);
            }
        }
        return new Snapshot(reviewsSnap, ordersSnap, collectionBooksSnap);
    }

    private void restore(Snapshot snap) {
        if (!snap.reviews().isEmpty()) {
            jdbc.batchUpdate("INSERT INTO reviews (id, book_id, user_id, rating, text, status, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (book_id, user_id) DO NOTHING", snap.reviews());
        }
        if (!snap.orders().isEmpty()) {
            jdbc.batchUpdate("INSERT INTO orders (id, user_id, book_id, status, created_at) " +
                    "VALUES (?, ?, ?, ?, ?) ON CONFLICT (user_id, book_id) DO NOTHING", snap.orders());
        }
        if (!snap.collectionBooks().isEmpty()) {
            jdbc.batchUpdate("INSERT INTO collection_books (collection_id, book_id, position) " +
                    "VALUES (?, ?, ?) ON CONFLICT DO NOTHING", snap.collectionBooks());
        }
    }

    private String summary(List<EvaluationReport.AlgorithmMetrics> metrics) {
        StringBuilder sb = new StringBuilder();
        for (EvaluationReport.AlgorithmMetrics m : metrics) {
            sb.append(m.getAlgorithm()).append("={");
            for (Map.Entry<String, Double> e : m.getValues().entrySet()) {
                sb.append(e.getKey()).append('=').append(String.format("%.3f", e.getValue())).append(' ');
            }
            sb.append("} ");
        }
        return sb.toString();
    }
}
