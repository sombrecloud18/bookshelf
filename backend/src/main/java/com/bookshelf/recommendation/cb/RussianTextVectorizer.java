package com.bookshelf.recommendation.cb;

import com.bookshelf.config.RecommendationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps Lucene's {@link RussianAnalyzer}, which performs morphological
 * stemming (учить / учится / учением → один корень), lowercase conversion
 * and stop-word filtering. Used to tokenize book descriptions for the
 * TF-IDF content-based filter.
 *
 * Lucene Analyzer is thread-safe per-thread, not across threads — we create
 * one shared analyzer instance and the per-call TokenStream owns its state.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RussianTextVectorizer {

    private final RecommendationProperties props;
    private final Analyzer analyzer = new RussianAnalyzer();

    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> tokens = new ArrayList<>();
        int minLen = props.getCb().getMinTokenLength();
        try (TokenStream ts = analyzer.tokenStream("text", new StringReader(text))) {
            CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
            ts.reset();
            while (ts.incrementToken()) {
                String t = term.toString();
                if (t.length() >= minLen) tokens.add(t);
            }
            ts.end();
        } catch (Exception e) {
            log.warn("Tokenizer error on text snippet ('{}...'): {}",
                    text.substring(0, Math.min(40, text.length())), e.getMessage());
        }
        return tokens;
    }
}
