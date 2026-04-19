package com.bookshelf.repository;

import com.bookshelf.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    @Query("SELECT e FROM Event e WHERE e.date >= CURRENT_DATE ORDER BY e.date ASC")
    Page<Event> findUpcoming(Pageable pageable);

    long countByDateGreaterThanEqual(LocalDate date);
}
