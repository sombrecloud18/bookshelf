package com.bookshelf.recommendation.data;

import java.util.List;
import java.util.Map;

/**
 * Static catalog of the 5 user archetypes used to drive synthetic data
 * generation. Weights are tuned against the books seeded by V3+V5+V10:
 * each persona has a clear "core" of preferred genres/authors with smaller
 * cross-genre interest so the generated matrix is not perfectly block-diagonal.
 */
public final class PersonaCatalog {

    private PersonaCatalog() {}

    public static final PersonaProfile HUMANITARIAN = new PersonaProfile(
            "humanitarian", "Гуманитарий",
            Map.of(
                    "Роман",            1.00,
                    "Роман-эпопея",     0.95,
                    "Роман в стихах",   0.90,
                    "Поэма",            0.85,
                    "Антиутопия",       0.75,
                    "Сказка",           0.60,
                    "Научно-популярное",0.25
            ),
            Map.of(
                    "Толстой",     0.30,
                    "Достоевский", 0.30,
                    "Булгаков",    0.25,
                    "Пушкин",      0.20,
                    "Ремарк",      0.20,
                    "Пастернак",   0.20,
                    "Шолохов",     0.20
            ),
            List.of("Роман", "Антиутопия", "Поэма"),
            List.of("Толстой", "Достоевский", "Булгаков")
    );

    public static final PersonaProfile PROGRAMMER = new PersonaProfile(
            "programmer", "Программист",
            Map.of(
                    "Программирование",     1.00,
                    "Базы данных",          0.80,
                    "Операционные системы", 0.55,
                    "Безопасность",         0.30,
                    "ИИ",                   0.30,
                    "Научно-популярное",    0.15
            ),
            Map.of(
                    "Мартин",      0.30,
                    "Макконнелл",  0.30,
                    "Фаулер",      0.25,
                    "Блох",        0.25,
                    "Кормен",      0.20,
                    "Бхаргава",    0.20,
                    "Уоллс",       0.15
            ),
            List.of("Программирование", "Базы данных"),
            List.of("Мартин", "Макконнелл", "Фаулер")
    );

    public static final PersonaProfile AI_ENGINEER = new PersonaProfile(
            "ai_engineer", "ИИ-инженер",
            Map.of(
                    "ИИ",                1.00,
                    "Программирование",  0.65,
                    "Базы данных",       0.40,
                    "Научно-популярное", 0.35
            ),
            Map.of(
                    "Жерон",     0.30,
                    "Гудфеллоу", 0.30,
                    "Бишоп",     0.25,
                    "Маккинни",  0.25,
                    "Саттон",    0.20,
                    "Мюллер",    0.20,
                    "Юрафски",   0.20
            ),
            List.of("ИИ", "Программирование"),
            List.of("Жерон", "Гудфеллоу", "Бишоп")
    );

    public static final PersonaProfile SECURITY = new PersonaProfile(
            "security", "Безопасник",
            Map.of(
                    "Безопасность",         1.00,
                    "Операционные системы", 0.65,
                    "Программирование",     0.40,
                    "Триллер",              0.35,
                    "Базы данных",          0.25
            ),
            Map.of(
                    "Шнайер",  0.30,
                    "Митник",  0.30,
                    "Форшоу",  0.25,
                    "Эриксон", 0.25,
                    "Таненбаум", 0.20,
                    "Лукас",   0.20,
                    "Браун",   0.15
            ),
            List.of("Безопасность", "Операционные системы"),
            List.of("Шнайер", "Митник")
    );

    public static final PersonaProfile GENERALIST = new PersonaProfile(
            "generalist", "Универсал",
            Map.of(
                    "Роман",                0.50,
                    "Антиутопия",           0.45,
                    "Программирование",     0.45,
                    "Научно-популярное",    0.55,
                    "Сказка",               0.40,
                    "ИИ",                   0.35,
                    "Безопасность",         0.30,
                    "Операционные системы", 0.30,
                    "Базы данных",          0.30,
                    "Триллер",              0.35
            ),
            Map.of(
                    "Хокинг",  0.20,
                    "Харари",  0.20,
                    "Канеман", 0.20,
                    "Талеб",   0.15
            ),
            List.of("Научно-популярное", "Роман"),
            List.of()
    );

    public static final List<PersonaProfile> ALL = List.of(
            HUMANITARIAN, PROGRAMMER, AI_ENGINEER, SECURITY, GENERALIST
    );

    /** Distribution: 25% humanitarian, 25% programmer, 20% ai, 15% security, 15% generalist. */
    public static final double[] DISTRIBUTION = { 0.25, 0.25, 0.20, 0.15, 0.15 };
}
