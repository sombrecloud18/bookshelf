package com.bookshelf.repository;

import com.bookshelf.entity.EventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, EventParticipant.EventParticipantId> {

    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);
    long countByEventId(UUID eventId);

    @Modifying
    @Query("DELETE FROM EventParticipant ep WHERE ep.event.id = :eventId AND ep.user.id = :userId")
    void deleteByEventIdAndUserId(@Param("eventId") UUID eventId, @Param("userId") UUID userId);
}
