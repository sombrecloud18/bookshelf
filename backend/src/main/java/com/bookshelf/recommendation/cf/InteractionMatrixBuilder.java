package com.bookshelf.recommendation.cf;

import com.bookshelf.config.RecommendationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Constructs the user×book rating matrix by aggregating positive-signal
 * tables. Signal-to-rating mapping is configurable in
 * {@link RecommendationProperties.Weights}:
 *
 * <ul>
 *   <li>Approved review → rating × multiplier (default: explicit 1..5)</li>
 *   <li>Book in any collection → {@code weights.collection} (default 4.5)</li>
 *   <li>Active order → {@code weights.order} (default 4.0)</li>
 *   <li>View activity → {@code weights.view} (default 1.0)</li>
 * </ul>
 *
 * When several signals exist for the same (user, book), the maximum is kept.
 * Reads happen in 4 fast SQL queries — no JPA hydration overhead.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InteractionMatrixBuilder {

    private final JdbcTemplate jdbc;
    private final RecommendationProperties props;

    public InteractionMatrix build() {
        InteractionMatrix m = new InteractionMatrix();
        var w = props.getWeights();

        // 1) Explicit reviews (approved only)
        List<Map<String, Object>> reviews = jdbc.queryForList(
                "SELECT user_id, book_id, rating FROM reviews WHERE status = 'APPROVED'");
        for (Map<String, Object> row : reviews) {
            m.put((UUID) row.get("user_id"), (UUID) row.get("book_id"),
                    ((Number) row.get("rating")).doubleValue() * w.getReviewMultiplier());
        }

        // 2) Active orders
        List<Map<String, Object>> orders = jdbc.queryForList(
                "SELECT user_id, book_id FROM orders WHERE status = 'ACTIVE'");
        for (Map<String, Object> row : orders) {
            m.put((UUID) row.get("user_id"), (UUID) row.get("book_id"), w.getOrder());
        }

        // 3) Books in approved/draft collections
        List<Map<String, Object>> coll = jdbc.queryForList(
                "SELECT c.user_id AS user_id, cb.book_id AS book_id " +
                        "FROM collection_books cb " +
                        "JOIN collections c ON c.id = cb.collection_id " +
                        "WHERE c.status IN ('APPROVED','DRAFT')");
        for (Map<String, Object> row : coll) {
            m.put((UUID) row.get("user_id"), (UUID) row.get("book_id"), w.getCollection());
        }

        // 4) View activity (only VIEW; ORDER and REVIEW types are auto-logged and
        //     already handled via their explicit tables above)
        List<Map<String, Object>> views = jdbc.queryForList(
                "SELECT user_id, book_id FROM user_activity WHERE activity_type = 'VIEW'");
        for (Map<String, Object> row : views) {
            m.put((UUID) row.get("user_id"), (UUID) row.get("book_id"), w.getView());
        }

        log.info("Матрица взаимодействий собрана: users={}, books={}, nnz={}",
                m.users().size(), m.allBooks().size(), m.totalNonZero());
        return m;
    }
}
