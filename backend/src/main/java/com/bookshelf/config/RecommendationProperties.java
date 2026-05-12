package com.bookshelf.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration for the hybrid recommendation engine.
 * Reads {@code recommendation.*} properties from application.properties.
 */
@Data
@ConfigurationProperties(prefix = "recommendation")
public class RecommendationProperties {

    private Synthetic synthetic = new Synthetic();
    private Weights weights = new Weights();
    private Cf cf = new Cf();
    private Cb cb = new Cb();
    private Hybrid hybrid = new Hybrid();
    private Eval eval = new Eval();

    @Data
    public static class Synthetic {
        private boolean enabled = false;
        private int users = 100;
        private int interactionsMean = 25;
        private int interactionsMin = 10;
        private int interactionsMax = 50;
        private double noiseRatio = 0.08;
        private long seed = 42L;
    }

    @Data
    public static class Weights {
        private double reviewMultiplier = 1.0;
        private double collection = 4.5;
        private double order = 4.0;
        private double view = 1.0;
    }

    @Data
    public static class Cf {
        private int topKNeighbors = 50;
        private int minCommonUsers = 2;
        private String refreshCron = "0 0 3 * * *";
    }

    @Data
    public static class Cb {
        private double alphaText = 0.6;
        private double betaGenre = 0.25;
        private double gammaAuthor = 0.15;
        private int minTokenLength = 3;
        private int maxTermsPerBook = 80;
    }

    @Data
    public static class Hybrid {
        private int coldThreshold = 3;
        private int warmThreshold = 10;
        private double lambdaCold = 0.0;
        private double lambdaWarm = 0.3;
        private double lambdaHot = 0.7;
    }

    @Data
    public static class Eval {
        private double trainRatio = 0.8;
        private int topK = 10;
    }
}
