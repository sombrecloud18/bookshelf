package com.bookshelf.recommendation.cf;

import com.bookshelf.config.RecommendationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * Computes the symmetric item × item cosine-similarity matrix from the
 * user×book rating matrix and persists the top-K neighbors per book into
 * {@code book_similarity}.
 *
 * <pre>
 *   sim(b1, b2) = (Σ rᵤ,b1 · rᵤ,b2)  /  (||r·,b1||₂ · ||r·,b2||₂)
 * </pre>
 *
 * Pairs with fewer than {@code recommendation.cf.min-common-users} co-rating
 * users are filtered out to avoid spurious similarities. Top-K (default 50)
 * neighbors are kept per book.
 *
 * Algorithmic complexity: O(B²·U_max), where B is the number of books and
 * U_max is the average users-per-book. On our scale (~50 books × ~100 users)
 * this is sub-second.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CosineItemSimilarityService {

    private final JdbcTemplate jdbc;
    private final InteractionMatrixBuilder builder;
    private final RecommendationProperties props;

    @Transactional
    public int rebuild() {
        long t0 = System.currentTimeMillis();
        InteractionMatrix matrix = builder.build();
        Map<UUID, Map<UUID, Double>> bookIdx = matrix.invertedIndex();

        // Pre-compute L2 norms
        Map<UUID, Double> norms = new HashMap<>();
        for (Map.Entry<UUID, Map<UUID, Double>> e : bookIdx.entrySet()) {
            double sumSq = 0;
            for (double v : e.getValue().values()) sumSq += v * v;
            norms.put(e.getKey(), Math.sqrt(sumSq));
        }

        List<UUID> bookIds = new ArrayList<>(bookIdx.keySet());
        int K = props.getCf().getTopKNeighbors();
        int minCommon = props.getCf().getMinCommonUsers();

        // For each book, accumulate top-K neighbors via bounded heap
        Map<UUID, PriorityQueue<ScoredBook>> heaps = new HashMap<>();
        for (UUID b : bookIds) heaps.put(b, new PriorityQueue<>(Comparator.comparingDouble(ScoredBook::score)));

        for (int i = 0; i < bookIds.size(); i++) {
            UUID b1 = bookIds.get(i);
            Map<UUID, Double> v1 = bookIdx.get(b1);
            double n1 = norms.get(b1);
            if (n1 == 0) continue;

            for (int j = i + 1; j < bookIds.size(); j++) {
                UUID b2 = bookIds.get(j);
                Map<UUID, Double> v2 = bookIdx.get(b2);
                double n2 = norms.get(b2);
                if (n2 == 0) continue;

                // Dot product over the smaller dimension
                Map<UUID, Double> smaller = v1.size() <= v2.size() ? v1 : v2;
                Map<UUID, Double> larger = smaller == v1 ? v2 : v1;
                int common = 0;
                double dot = 0;
                for (Map.Entry<UUID, Double> e : smaller.entrySet()) {
                    Double w = larger.get(e.getKey());
                    if (w != null) {
                        dot += e.getValue() * w;
                        common++;
                    }
                }
                if (common < minCommon || dot == 0) continue;

                double sim = dot / (n1 * n2);
                if (sim <= 0) continue;

                pushHeap(heaps.get(b1), new ScoredBook(b2, sim), K);
                pushHeap(heaps.get(b2), new ScoredBook(b1, sim), K);
            }
        }

        // Bulk overwrite book_similarity
        jdbc.update("DELETE FROM book_similarity");
        List<Object[]> batch = new ArrayList<>();
        Timestamp now = Timestamp.from(Instant.now());
        for (Map.Entry<UUID, PriorityQueue<ScoredBook>> e : heaps.entrySet()) {
            for (ScoredBook sb : e.getValue()) {
                batch.add(new Object[]{ e.getKey(), sb.bookId(), sb.score(), now });
            }
        }
        if (!batch.isEmpty()) {
            jdbc.batchUpdate(
                    "INSERT INTO book_similarity (book_id_1, book_id_2, similarity, computed_at) " +
                            "VALUES (?, ?, ?, ?) ON CONFLICT (book_id_1, book_id_2) DO NOTHING",
                    batch);
        }

        long elapsed = System.currentTimeMillis() - t0;
        log.info("Пересчёт book_similarity: rows={}, books_covered={}, elapsed={}мс",
                batch.size(), heaps.size(), elapsed);
        return batch.size();
    }

    private static void pushHeap(PriorityQueue<ScoredBook> heap, ScoredBook item, int K) {
        if (heap.size() < K) {
            heap.offer(item);
        } else if (!heap.isEmpty() && item.score() > heap.peek().score()) {
            heap.poll();
            heap.offer(item);
        }
    }
}
