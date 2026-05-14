-- =====================================================================
-- V3: Recommendation engine schema
-- Tables to support hybrid recommendation system:
--   * book_similarity         — item-item cosine similarity matrix (CF)
--   * book_tfidf              — sparse TF-IDF term weights per book (CB)
--   * recommendation_log      — every recommendation served (analytics)
--   * recommendation_evaluation — offline metrics (P@K, R@K, NDCG, etc.)
-- =====================================================================

CREATE TABLE book_similarity (
    book_id_1   UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    book_id_2   UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    similarity  DOUBLE PRECISION NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (book_id_1, book_id_2),
    CHECK (book_id_1 <> book_id_2),
    CHECK (similarity >= -1.0 AND similarity <= 1.0)
);

CREATE INDEX idx_book_similarity_top_neighbors
    ON book_similarity (book_id_1, similarity DESC);

CREATE TABLE book_tfidf (
    book_id UUID         NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    term    VARCHAR(100) NOT NULL,
    weight  REAL         NOT NULL,
    PRIMARY KEY (book_id, term)
);

CREATE INDEX idx_book_tfidf_book ON book_tfidf (book_id);
CREATE INDEX idx_book_tfidf_term ON book_tfidf (term);

CREATE TABLE recommendation_log (
    id       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id  UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    source   VARCHAR(20)      NOT NULL,
    score    DOUBLE PRECISION,
    shown_at TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    clicked  BOOLEAN          NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_rec_log_user   ON recommendation_log (user_id, shown_at DESC);
CREATE INDEX idx_rec_log_source ON recommendation_log (source);

CREATE TABLE recommendation_evaluation (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    run_id       UUID             NOT NULL,
    algorithm    VARCHAR(40)      NOT NULL,
    metric_name  VARCHAR(40)      NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    config_json  TEXT,
    computed_at  TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rec_eval_run ON recommendation_evaluation (run_id);
