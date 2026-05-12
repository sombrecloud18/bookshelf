package com.bookshelf.recommendation.cb;

import com.bookshelf.entity.Book;
import com.bookshelf.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rebuilds the {@code book_tfidf} table by running every active book's
 * (title + author + genre + description + full_description) text through
 * the Russian tokenizer + TF-IDF calculator, then bulk-inserting the
 * resulting sparse vectors.
 *
 * Also exposes an in-memory cache loader for downstream services so we
 * don't re-query the DB on every recommendation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookVectorService {

    private final BookRepository bookRepository;
    private final RussianTextVectorizer vectorizer;
    private final TfIdfCalculator tfIdf;
    private final JdbcTemplate jdbc;

    @Transactional
    public int rebuildAll() {
        long t0 = System.currentTimeMillis();
        List<Book> books = bookRepository.findAll();
        List<List<String>> tokenized = new ArrayList<>(books.size());
        for (Book b : books) tokenized.add(vectorizer.tokenize(combineText(b)));

        List<Map<String, Double>> vectors = tfIdf.computeCorpus(tokenized);

        jdbc.update("DELETE FROM book_tfidf");
        List<Object[]> batch = new ArrayList<>();
        for (int i = 0; i < books.size(); i++) {
            UUID id = books.get(i).getId();
            for (Map.Entry<String, Double> e : vectors.get(i).entrySet()) {
                batch.add(new Object[]{ id, e.getKey(), e.getValue().floatValue() });
            }
        }
        if (!batch.isEmpty()) {
            jdbc.batchUpdate(
                    "INSERT INTO book_tfidf (book_id, term, weight) VALUES (?, ?, ?) " +
                            "ON CONFLICT (book_id, term) DO NOTHING",
                    batch);
        }
        long elapsed = System.currentTimeMillis() - t0;
        log.info("book_tfidf пересчитана: books={}, terms_total={}, elapsed={}мс",
                books.size(), batch.size(), elapsed);
        return batch.size();
    }

    /**
     * Loads all TF-IDF vectors into an in-memory map. Caller should hold the
     * result for the duration of a single scoring pass.
     */
    public Map<UUID, Map<String, Double>> loadAllVectors() {
        Map<UUID, Map<String, Double>> all = new HashMap<>();
        jdbc.query("SELECT book_id, term, weight FROM book_tfidf", rs -> {
            UUID id = (UUID) rs.getObject("book_id");
            all.computeIfAbsent(id, k -> new HashMap<>())
                    .put(rs.getString("term"), (double) rs.getFloat("weight"));
        });
        return all;
    }

    private String combineText(Book b) {
        StringBuilder sb = new StringBuilder();
        if (b.getTitle() != null) sb.append(b.getTitle()).append(' ');
        if (b.getAuthor() != null) sb.append(b.getAuthor()).append(' ');
        if (b.getGenre() != null) sb.append(b.getGenre()).append(' ');
        if (b.getDescription() != null) sb.append(b.getDescription()).append(' ');
        if (b.getFullDescription() != null) sb.append(b.getFullDescription());
        return sb.toString();
    }
}
