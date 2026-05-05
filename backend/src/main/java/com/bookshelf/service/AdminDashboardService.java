package com.bookshelf.service;

import com.bookshelf.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final CollectionRepository collectionRepository;
    private final SubjectCollectionRepository subjectCollectionRepository;
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        log.debug("Сбор статистики для дашборда");
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalUsers", userRepository.count());
        stats.put("totalModerators", userRepository.countByRole("MODERATOR"));
        stats.put("totalBooks", bookRepository.count());
        stats.put("pendingReviews", reviewRepository.countByStatus("PENDING"));
        stats.put("approvedReviews", reviewRepository.countByStatus("APPROVED"));
        stats.put("pendingCollections", collectionRepository.countByStatus("PENDING"));
        stats.put("approvedCollections", collectionRepository.countByStatus("APPROVED"));
        stats.put("pendingSubjectCollections", subjectCollectionRepository.countByStatus("PENDING"));
        stats.put("approvedSubjectCollections", subjectCollectionRepository.countByStatus("APPROVED"));
        stats.put("upcomingEvents", eventRepository.countByDateGreaterThanEqual(LocalDate.now()));

        return stats;
    }
}
