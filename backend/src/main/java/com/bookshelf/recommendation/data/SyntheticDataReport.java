package com.bookshelf.recommendation.data;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Report returned after a synthetic data generation run.
 * Used both as a REST response and for log-line diagnostics.
 */
@Data
@Builder
public class SyntheticDataReport {
    private long seed;
    private int totalUsers;
    private int totalStudents;
    private int totalTeachers;
    private Map<String, Integer> usersPerPersona;
    private int totalActivities;
    private int totalReviews;
    private int totalOrders;
    private int totalCollections;
    private int totalCollectionBooks;
    private int totalSubjectCollections;
    private double interactionDensity;   // activities / (users × books)
    private long elapsedMs;
    private String message;
}
