package com.bookshelf.recommendation.eval;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregated offline-evaluation result. Each entry under {@code metrics} maps
 * an algorithm name (RANDOM / POPULAR / CF / CB / HYBRID) to its computed
 * metric values.
 */
@Data
@Builder
public class EvaluationReport {
    private UUID runId;
    private int topK;
    private double trainRatio;
    private int trainSize;
    private int testSize;
    private int usersEvaluated;
    private List<AlgorithmMetrics> metrics;
    private long elapsedMs;

    @Data
    @Builder
    public static class AlgorithmMetrics {
        private String algorithm;
        private Map<String, Double> values;   // precision@K, recall@K, ndcg@K, coverage, diversity
    }
}
