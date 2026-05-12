package com.bookshelf.controller.admin;

import com.bookshelf.recommendation.cb.BookVectorService;
import com.bookshelf.recommendation.cf.CosineItemSimilarityService;
import com.bookshelf.recommendation.data.SyntheticDataGenerator;
import com.bookshelf.recommendation.data.SyntheticDataReport;
import com.bookshelf.recommendation.eval.EvaluationReport;
import com.bookshelf.recommendation.eval.EvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-facing endpoints for the recommendation engine.
 *
 * Protected by {@code /api/admin/**} → {@code hasRole('MODERATOR')} in
 * SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin/recommendation")
@RequiredArgsConstructor
@Slf4j
public class RecommendationAdminController {

    private final SyntheticDataGenerator generator;
    private final CosineItemSimilarityService cfRebuild;
    private final BookVectorService cbVectors;
    private final EvaluationService evaluation;

    /** Wipes prior synthetic data and regenerates the dataset. */
    @PostMapping("/seed")
    public ResponseEntity<SyntheticDataReport> seed(@RequestBody(required = false) Map<String, Integer> body) {
        int n = (body != null && body.containsKey("users")) ? body.get("users") : 100;
        if (n < 1 || n > 1000) return ResponseEntity.badRequest().build();
        log.info("Запрос на генерацию синтетики: users={}", n);
        return ResponseEntity.ok(generator.generate(n));
    }

    /** Wipes all synthetic users (login prefix {@code synth_}). */
    @DeleteMapping("/seed")
    public ResponseEntity<Map<String, Integer>> wipe() {
        return ResponseEntity.ok(Map.of("deletedUsers", generator.wipeSynthetic()));
    }

    /** Forces rebuild of the CF book-similarity matrix. */
    @PostMapping("/rebuild-cf")
    public ResponseEntity<Map<String, Integer>> rebuildCf() {
        log.info("Принудительный пересчёт book_similarity");
        return ResponseEntity.ok(Map.of("rows", cfRebuild.rebuild()));
    }

    /** Forces rebuild of CB TF-IDF vectors. */
    @PostMapping("/rebuild-cb")
    public ResponseEntity<Map<String, Integer>> rebuildCb() {
        log.info("Принудительный пересчёт book_tfidf");
        return ResponseEntity.ok(Map.of("rows", cbVectors.rebuildAll()));
    }

    /** Runs full offline evaluation (RANDOM / POPULAR / CF / CB / HYBRID). */
    @PostMapping("/evaluate")
    public ResponseEntity<EvaluationReport> evaluate() {
        log.info("Запуск offline-evaluation");
        return ResponseEntity.ok(evaluation.runFullEvaluation());
    }
}
