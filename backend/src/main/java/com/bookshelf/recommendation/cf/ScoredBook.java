package com.bookshelf.recommendation.cf;

import java.util.UUID;

/**
 * Pair of book id and a numeric score, returned by every scoring service
 * (CF, CB, Hybrid). Implements {@link Comparable} for natural descending sort.
 */
public record ScoredBook(UUID bookId, double score) implements Comparable<ScoredBook> {
    @Override
    public int compareTo(ScoredBook o) {
        return Double.compare(o.score, this.score);
    }
}
