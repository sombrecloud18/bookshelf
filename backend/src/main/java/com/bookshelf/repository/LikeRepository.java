package com.bookshelf.repository;

import com.bookshelf.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LikeRepository extends JpaRepository<Like, UUID> {

    Optional<Like> findByUserIdAndTargetTypeAndTargetId(UUID userId, String targetType, UUID targetId);

    long countByTargetTypeAndTargetId(String targetType, UUID targetId);

    @Query("SELECT l.targetId FROM Like l " +
           "WHERE l.userId = :userId AND l.targetType = :targetType AND l.targetId IN :ids")
    List<UUID> findLikedTargetIds(@Param("userId") UUID userId,
                                  @Param("targetType") String targetType,
                                  @Param("ids") Collection<UUID> ids);

    @Modifying
    @Transactional
    @Query("DELETE FROM Like l WHERE l.targetType = :targetType AND l.targetId = :targetId")
    void deleteAllByTarget(@Param("targetType") String targetType, @Param("targetId") UUID targetId);
}
