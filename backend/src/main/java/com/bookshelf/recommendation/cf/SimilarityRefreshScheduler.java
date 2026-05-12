package com.bookshelf.recommendation.cf;

import com.bookshelf.recommendation.cb.BookVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers nightly recomputation of the CF item-similarity matrix and the
 * CB TF-IDF vectors. Also kicks off a first build on application startup
 * if either table is empty — this ensures the system is usable immediately
 * after a fresh seed without waiting for 3:00 AM.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SimilarityRefreshScheduler {

    private final CosineItemSimilarityService cfService;
    private final BookVectorService cbVectorService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try {
            Integer simCount = jdbc.queryForObject("SELECT COUNT(*) FROM book_similarity", Integer.class);
            Integer tfidfCount = jdbc.queryForObject("SELECT COUNT(*) FROM book_tfidf", Integer.class);
            log.info("При старте: book_similarity rows={}, book_tfidf rows={}", simCount, tfidfCount);
            if (simCount != null && simCount == 0) {
                log.info("book_similarity пуста — запускаю первичный пересчёт");
                cfService.rebuild();
            }
            if (tfidfCount != null && tfidfCount == 0) {
                log.info("book_tfidf пуста — запускаю первичный пересчёт");
                cbVectorService.rebuildAll();
            }
        } catch (Exception e) {
            log.warn("Не удалось проверить состояние рекомендательных таблиц при старте: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "${recommendation.cf.refresh-cron:0 0 3 * * *}")
    public void nightlyRefresh() {
        log.info("Ночной пересчёт рекомендательных матриц — старт");
        try {
            cfService.rebuild();
            cbVectorService.rebuildAll();
            log.info("Ночной пересчёт завершён");
        } catch (Exception e) {
            log.error("Ночной пересчёт упал: {}", e.getMessage(), e);
        }
    }
}
