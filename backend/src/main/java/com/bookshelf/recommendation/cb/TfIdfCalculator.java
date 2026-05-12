package com.bookshelf.recommendation.cb;

import com.bookshelf.config.RecommendationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes TF-IDF weights for a corpus of token lists.
 *
 * <pre>
 *   tf(t, d)  = count(t in d) / |d|
 *   idf(t)    = log( (1 + N) / (1 + df(t)) ) + 1   (smoothed)
 *   tfidf(t,d)= tf(t,d) · idf(t)                    (L2-normalised per doc)
 * </pre>
 *
 * Returns a sparse map per document; vectors are L2-normalised so cosine
 * similarity reduces to a dot product downstream.
 */
@Component
@RequiredArgsConstructor
public class TfIdfCalculator {

    private final RecommendationProperties props;

    /**
     * @param docs ordered list of tokenised documents
     * @return one map per document (insertion order matches input)
     */
    public List<Map<String, Double>> computeCorpus(List<List<String>> docs) {
        int N = docs.size();
        Map<String, Integer> df = new HashMap<>();
        for (List<String> doc : docs) {
            for (String t : new java.util.HashSet<>(doc)) df.merge(t, 1, Integer::sum);
        }
        Map<String, Double> idf = new HashMap<>();
        for (Map.Entry<String, Integer> e : df.entrySet()) {
            idf.put(e.getKey(), Math.log((1.0 + N) / (1.0 + e.getValue())) + 1.0);
        }

        List<Map<String, Double>> out = new java.util.ArrayList<>(N);
        int maxTerms = props.getCb().getMaxTermsPerBook();
        for (List<String> doc : docs) {
            if (doc.isEmpty()) {
                out.add(Map.of());
                continue;
            }
            Map<String, Integer> tf = new HashMap<>();
            for (String t : doc) tf.merge(t, 1, Integer::sum);
            double size = doc.size();

            Map<String, Double> raw = new HashMap<>();
            for (Map.Entry<String, Integer> e : tf.entrySet()) {
                double w = (e.getValue() / size) * idf.getOrDefault(e.getKey(), 0.0);
                if (w > 0) raw.put(e.getKey(), w);
            }

            // Keep top-N terms by weight to bound storage
            Map<String, Double> top = raw.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(maxTerms)
                    .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), LinkedHashMap::putAll);

            // L2-normalise
            double sumSq = 0;
            for (double v : top.values()) sumSq += v * v;
            double norm = Math.sqrt(sumSq);
            if (norm > 0) {
                top.replaceAll((k, v) -> v / norm);
            }
            out.add(top);
        }
        return out;
    }

    /** Cosine of two normalised sparse vectors = dot product. */
    public static double cosine(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        Map<String, Double> smaller = a.size() <= b.size() ? a : b;
        Map<String, Double> larger = smaller == a ? b : a;
        double dot = 0;
        for (Map.Entry<String, Double> e : smaller.entrySet()) {
            Double w = larger.get(e.getKey());
            if (w != null) dot += e.getValue() * w;
        }
        return dot;
    }

    /** Adds vector {@code b·scale} into accumulator {@code acc}. */
    public static void accumulate(Map<String, Double> acc, Map<String, Double> b, double scale) {
        for (Map.Entry<String, Double> e : b.entrySet()) {
            acc.merge(e.getKey(), e.getValue() * scale, Double::sum);
        }
    }

    /** L2-normalises a sparse vector in place. */
    public static void normalise(Map<String, Double> v) {
        double sumSq = 0;
        for (double x : v.values()) sumSq += x * x;
        double n = Math.sqrt(sumSq);
        if (n > 0) v.replaceAll((k, x) -> x / n);
    }

    public static Comparator<Map.Entry<String, Double>> byWeightDesc() {
        return Map.Entry.<String, Double>comparingByValue().reversed();
    }
}
