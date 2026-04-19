package com.bookshelf.repository;

import com.bookshelf.entity.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserActivityRepository extends JpaRepository<UserActivity, UUID> {

    List<UserActivity> findByUserId(UUID userId);

    @Query(value = "SELECT ua2.book_id, COUNT(*) as cnt FROM user_activity ua1 " +
                   "JOIN user_activity ua2 ON ua1.user_id = ua2.user_id AND ua2.book_id != ua1.book_id " +
                   "WHERE ua1.book_id IN (SELECT book_id FROM user_activity WHERE user_id = CAST(:userId AS uuid)) " +
                   "AND ua2.book_id NOT IN (SELECT book_id FROM user_activity WHERE user_id = CAST(:userId AS uuid)) " +
                   "GROUP BY ua2.book_id ORDER BY cnt DESC LIMIT :limit",
           nativeQuery = true)
    List<Object[]> findCollaborativeRecommendations(@Param("userId") String userId, @Param("limit") int limit);
}
