package com.bookshelf.repository;

import com.bookshelf.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {

    Page<Book> findByGenre(String genre, Pageable pageable);

    List<Book> findByIdIn(List<UUID> ids);

    @Query(value = "SELECT * FROM books WHERE search_vector @@ plainto_tsquery('russian', :query) " +
                   "ORDER BY ts_rank(search_vector, plainto_tsquery('russian', :query)) DESC",
           countQuery = "SELECT count(*) FROM books WHERE search_vector @@ plainto_tsquery('russian', :query)",
           nativeQuery = true)
    Page<Book> searchByText(@Param("query") String query, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(b.author) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<Book> searchByTitleOrAuthor(@Param("q") String query, Pageable pageable);

    @Query(value = "SELECT b.* FROM books b " +
                   "JOIN reviews r ON r.book_id = b.id AND r.status = 'APPROVED' " +
                   "GROUP BY b.id ORDER BY AVG(r.rating) DESC LIMIT :limit",
           nativeQuery = true)
    List<Book> findTopRatedBooks(@Param("limit") int limit);

    @Query(value = "SELECT * FROM books WHERE id NOT IN " +
                   "(SELECT book_id FROM user_activity WHERE user_id = CAST(:userId AS uuid)) " +
                   "ORDER BY RANDOM() LIMIT :limit",
           nativeQuery = true)
    List<Book> findRandomUnreadBooks(@Param("userId") String userId, @Param("limit") int limit);

    @Query("SELECT b FROM Book b WHERE b.genre = :genre AND b.id != :excludeId ORDER BY b.createdAt DESC")
    List<Book> findSimilarByGenre(@Param("genre") String genre, @Param("excludeId") UUID excludeId, Pageable pageable);

    long count();
}
