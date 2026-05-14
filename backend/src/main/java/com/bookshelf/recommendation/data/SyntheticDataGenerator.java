package com.bookshelf.recommendation.data;

import com.bookshelf.config.RecommendationProperties;
import com.bookshelf.entity.Book;
import com.bookshelf.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Generates a realistic synthetic dataset for offline evaluation of the
 * recommendation engine. Uses a persona-based latent-preference model: each
 * synthetic user is assigned an archetype (see {@link PersonaCatalog}) and
 * samples books with probability proportional to persona affinity, plus
 * configurable noise.
 *
 * The pool is mixed: a configurable fraction of synthetic users
 * ({@code recommendation.synthetic.teacher-ratio}) are teachers — they get
 * a faculty + кафедра + position instead of a specialty + course, and they
 * tend to produce more curated COLLECTION-style activity than students.
 *
 * Faculty / specialty assignments use the real BSUIR bachelor taxonomy
 * seeded by V4 so that user.faculty / user.specialty values match what
 * the front end shows in the registration dropdowns.
 *
 * Usage: invoked through {@code POST /api/admin/recommendation/seed}
 * (MODERATOR-only). Idempotent — wipes previous synthetic data (login prefix
 * {@code synth_}) before regenerating.
 *
 * All randomness flows through a single seeded RNG so runs are reproducible.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyntheticDataGenerator {

    public static final String SYNTH_LOGIN_PREFIX = "synth_";

    // Real BSUIR bachelor specialties per faculty (mirrors V4 seed).
    private static final Map<String, String[]> FACULTY_TO_SPECIALTIES = Map.of(
            "ФРЭ", new String[]{
                    "Микро- и наноэлектроника",
                    "Радиосистемы и радиотехнологии",
                    "Нанотехнологии и наноматериалы",
                    "Инженерно-педагогическая деятельность"
            },
            "ИЭФ", new String[]{
                    "Информационные системы и технологии (в экономике)",
                    "Информационные системы и технологии (в логистике)",
                    "Информационные системы и технологии (в финансово-банковской сфере)",
                    "Электронная экономика",
                    "Цифровой маркетинг"
            },
            "ФКП", new String[]{
                    "Информационные системы и технологии (в обеспечении промышленной безопасности)",
                    "Информационные системы и технологии (в бизнес-менеджменте)",
                    "Компьютерная инженерия (программируемые мобильные системы)",
                    "Программная инженерия (инженерно-психологическое обеспечение ИТ)",
                    "Электронные системы и технологии",
                    "Электронное машиностроение"
            },
            "ФКСиС", new String[]{
                    "Компьютерная инженерия (вычислительные машины, системы и сети)",
                    "Компьютерная инженерия (встраиваемые системы)",
                    "Программная инженерия (программное обеспечение ИТ)",
                    "Информатика и технологии программирования"
            },
            "ФИБ", new String[]{
                    "Информационная безопасность (защита информации в телекоммуникациях)",
                    "Системы и сети инфокоммуникаций (программно-техническое обеспечение)",
                    "Системы и сети инфокоммуникаций (программное обеспечение инфокоммуникаций)"
            },
            "ФИТУ", new String[]{
                    "Информационные системы и технологии (в игровой индустрии)",
                    "Искусственный интеллект",
                    "Киберфизические системы",
                    "Системы управления информацией (АСОИ)",
                    "Промышленная электроника"
            },
            "ВФ", new String[]{
                    "Компьютерная инженерия (вычислительные системы спец. назначения)",
                    "Системы и сети инфокоммуникаций (системы телекоммуникаций спец. назначения)",
                    "Информационная безопасность (обеспечение безопасности ИТ)"
            }
    );

    // Realistic кафедра names per faculty (used for TEACHER accounts).
    private static final Map<String, String[]> FACULTY_TO_DEPARTMENTS = Map.of(
            "ФРЭ", new String[]{
                    "Кафедра радиотехнических систем",
                    "Кафедра микро- и наноэлектроники",
                    "Кафедра антенн и устройств СВЧ"
            },
            "ИЭФ", new String[]{
                    "Кафедра экономической информатики",
                    "Кафедра менеджмента",
                    "Кафедра цифровой экономики"
            },
            "ФКП", new String[]{
                    "Кафедра инженерной психологии и эргономики",
                    "Кафедра электронной техники и технологии",
                    "Кафедра проектирования информационных систем"
            },
            "ФКСиС", new String[]{
                    "Кафедра программного обеспечения информационных технологий",
                    "Кафедра электронных вычислительных машин",
                    "Кафедра информатики"
            },
            "ФИБ", new String[]{
                    "Кафедра защиты информации",
                    "Кафедра инфокоммуникационных технологий",
                    "Кафедра сетей и устройств телекоммуникаций"
            },
            "ФИТУ", new String[]{
                    "Кафедра интеллектуальных информационных технологий",
                    "Кафедра информационных технологий автоматизированных систем",
                    "Кафедра систем управления"
            },
            "ВФ", new String[]{
                    "Кафедра боевого применения средств радиоэлектронной борьбы",
                    "Кафедра военной разведки и связи",
                    "Кафедра тактики и общевоенных дисциплин"
            }
    );

    private static final String[] TEACHER_POSITIONS = {
            "Ассистент", "Старший преподаватель", "Доцент", "Профессор", "Заведующий кафедрой"
    };

    private static final String[] COURSES = { "1 курс", "2 курс", "3 курс", "4 курс" };
    private static final String[] FIRST_NAMES_MALE = {
            "Александр", "Андрей", "Дмитрий", "Иван", "Михаил", "Сергей", "Артём",
            "Павел", "Виктор", "Олег", "Николай", "Григорий"
    };
    private static final String[] FIRST_NAMES_FEMALE = {
            "Анна", "Мария", "Екатерина", "Ольга", "Юлия", "Наталья", "Виктория",
            "Татьяна", "Елена", "Светлана", "Ирина"
    };
    private static final String[] LAST_NAMES_MALE = {
            "Иванов", "Петров", "Сидоров", "Кузнецов", "Смирнов", "Попов",
            "Васильев", "Михайлов", "Новиков", "Фёдоров", "Морозов", "Волков"
    };
    private static final String[] LAST_NAMES_FEMALE = {
            "Иванова", "Петрова", "Сидорова", "Кузнецова", "Смирнова", "Попова",
            "Васильева", "Михайлова", "Новикова", "Фёдорова", "Морозова", "Волкова"
    };

    private final JdbcTemplate jdbc;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;
    private final RecommendationProperties props;

    /**
     * Wipes previously generated synthetic users (cascade deletes their
     * activities, reviews, orders, collections, preferences).
     */
    @Transactional
    public int wipeSynthetic() {
        int deleted = jdbc.update("DELETE FROM users WHERE login LIKE ?", SYNTH_LOGIN_PREFIX + '%');
        log.info("Удалены синтетические пользователи: {}", deleted);
        return deleted;
    }

    /**
     * Generates {@code numUsers} synthetic users with persona-driven activity.
     */
    @Transactional
    public SyntheticDataReport generate(int numUsers) {
        long start = System.currentTimeMillis();
        wipeSynthetic();

        Random rng = new Random(props.getSynthetic().getSeed());
        String passwordHash = passwordEncoder.encode("admin123");

        List<Book> books = bookRepository.findAll();
        if (books.isEmpty()) {
            throw new IllegalStateException("В каталоге нет книг — синтетику генерировать не на чем");
        }

        String[] facultyNames = FACULTY_TO_SPECIALTIES.keySet().toArray(new String[0]);
        double teacherRatio = props.getSynthetic().getTeacherRatio();

        Map<String, Integer> personaCounts = new HashMap<>();
        int totalStudents = 0;
        int totalTeachers = 0;
        int totalActivities = 0;
        int totalReviews = 0;
        int totalOrders = 0;
        int totalCollections = 0;
        int totalCollectionBooks = 0;
        int totalSubjectCollections = 0;

        // Pre-generate users so we can batch-insert
        List<Object[]> userBatch = new ArrayList<>();
        List<SyntheticUser> users = new ArrayList<>(numUsers);
        for (int i = 0; i < numUsers; i++) {
            PersonaProfile persona = pickPersona(rng);
            personaCounts.merge(persona.name(), 1, Integer::sum);

            boolean isTeacher = rng.nextDouble() < teacherRatio;
            boolean isFemale = rng.nextBoolean();
            String firstName = (isFemale ? FIRST_NAMES_FEMALE : FIRST_NAMES_MALE)
                    [rng.nextInt((isFemale ? FIRST_NAMES_FEMALE : FIRST_NAMES_MALE).length)];
            String lastName  = (isFemale ? LAST_NAMES_FEMALE  : LAST_NAMES_MALE)
                    [rng.nextInt((isFemale ? LAST_NAMES_FEMALE  : LAST_NAMES_MALE).length)];

            String faculty = facultyNames[rng.nextInt(facultyNames.length)];
            String specialty = null;
            String course = null;
            String department = null;
            String position = null;
            String userType;

            if (isTeacher) {
                userType = "TEACHER";
                String[] depts = FACULTY_TO_DEPARTMENTS.get(faculty);
                department = depts[rng.nextInt(depts.length)];
                position = TEACHER_POSITIONS[rng.nextInt(TEACHER_POSITIONS.length)];
                totalTeachers++;
            } else {
                userType = "STUDENT";
                String[] specs = FACULTY_TO_SPECIALTIES.get(faculty);
                specialty = specs[rng.nextInt(specs.length)];
                course = COURSES[rng.nextInt(COURSES.length)];
                totalStudents++;
            }

            UUID uid = UUID.randomUUID();
            String login = String.format(Locale.ROOT, "%s%s_%03d",
                    SYNTH_LOGIN_PREFIX, isTeacher ? "teacher" : "student", i + 1);
            String email = String.format(Locale.ROOT, "%s@bookshelf.dev", login);

            userBatch.add(new Object[]{
                    uid, login, email, passwordHash, firstName, lastName, null,
                    faculty, specialty, course, null, "USER", userType,
                    department, position, false
            });
            users.add(new SyntheticUser(uid, login, persona, isTeacher));
        }

        jdbc.batchUpdate(
                "INSERT INTO users (id, login, email, password_hash, first_name, last_name, patronymic, " +
                        "faculty, specialty, course, phone_number, role, user_type, department, position, is_blocked) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                userBatch);

        // user_preferences uses TEXT[] columns — bind arrays via Connection.createArrayOf(...).
        jdbc.execute((java.sql.Connection conn) -> {
            try (var ps = conn.prepareStatement(
                    "INSERT INTO user_preferences (id, user_id, favorite_genres, favorite_authors) " +
                            "VALUES (?, ?, ?, ?)")) {
                for (SyntheticUser u : users) {
                    ps.setObject(1, UUID.randomUUID());
                    ps.setObject(2, u.userId());
                    ps.setArray(3, conn.createArrayOf("text",
                            u.persona().favoriteGenres().toArray(new String[0])));
                    ps.setArray(4, conn.createArrayOf("text",
                            u.persona().favoriteAuthors().toArray(new String[0])));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return null;
        });

        // Now generate interactions
        List<Object[]> activityBatch = new ArrayList<>();
        List<Object[]> reviewBatch = new ArrayList<>();
        List<Object[]> orderBatch = new ArrayList<>();
        List<Object[]> collectionBatch = new ArrayList<>();
        List<Object[]> collectionBookBatch = new ArrayList<>();
        List<Object[]> subjectCollectionBatch = new ArrayList<>();
        List<Object[]> subjectCollectionBookBatch = new ArrayList<>();

        for (SyntheticUser u : users) {
            // Teachers a bit more active than students — they curate more.
            int meanBonus = u.teacher() ? 5 : 0;
            int interactions = clampPoisson(rng, props.getSynthetic().getInteractionsMean() + meanBonus,
                    props.getSynthetic().getInteractionsMin(), props.getSynthetic().getInteractionsMax());

            double[] scores = computeAffinities(u.persona(), books, rng);
            List<Book> selected = sampleWithoutReplacement(books, scores, interactions, rng);

            // Inject some noise interactions uncorrelated with persona.
            int noiseCount = (int) Math.round(interactions * props.getSynthetic().getNoiseRatio());
            for (int k = 0; k < noiseCount; k++) {
                Book noise = books.get(rng.nextInt(books.size()));
                if (!selected.contains(noise)) selected.add(noise);
            }

            UUID userCollectionId = null;
            UUID teacherSubjectCollectionId = null;

            for (Book b : selected) {
                String type = pickActivityType(rng, u.teacher());
                double affinity = persoBookScore(u.persona(), b);
                OffsetDateTime when = randomPastDate(rng, 60);
                Timestamp ts = Timestamp.from(when.toInstant());

                // Always record into user_activity (CF matrix builder reads this).
                activityBatch.add(new Object[]{ UUID.randomUUID(), u.userId(), b.getId(), type, ts });
                totalActivities++;

                switch (type) {
                    case "REVIEW" -> {
                        int rating = clamp((int) Math.round(1 + affinity * 4 + rng.nextGaussian() * 0.4), 1, 5);
                        reviewBatch.add(new Object[]{
                                UUID.randomUUID(), b.getId(), u.userId(),
                                rating, generateReviewText(b, rating, u.teacher()),
                                "APPROVED", ts, ts
                        });
                        totalReviews++;
                    }
                    case "ORDER" -> {
                        orderBatch.add(new Object[]{
                                UUID.randomUUID(), u.userId(), b.getId(), "ACTIVE", ts
                        });
                        totalOrders++;
                    }
                    case "COLLECTION" -> {
                        if (userCollectionId == null) {
                            userCollectionId = UUID.randomUUID();
                            collectionBatch.add(new Object[]{
                                    userCollectionId, u.userId(),
                                    "Моя подборка — " + u.persona().displayName(),
                                    u.persona().favoriteGenres().isEmpty()
                                            ? null : u.persona().favoriteGenres().get(0),
                                    "Личная подборка, сформированная автоматически на основе предпочтений.",
                                    "APPROVED", ts, ts
                            });
                            totalCollections++;
                        }
                        collectionBookBatch.add(new Object[]{
                                userCollectionId, b.getId(), totalCollectionBooks
                        });
                        totalCollectionBooks++;
                    }
                    case "SUBJECT_COLLECTION" -> {
                        if (teacherSubjectCollectionId == null) {
                            teacherSubjectCollectionId = UUID.randomUUID();
                            String subjectName = pickTeacherSubject(u.persona(), rng);
                            String specialtyName = "Программная инженерия (программное обеспечение ИТ)";
                            subjectCollectionBatch.add(new Object[]{
                                    teacherSubjectCollectionId, u.userId(),
                                    subjectName, specialtyName, specialtyName,
                                    "Рекомендую к курсу: " + subjectName,
                                    "Учебная подборка, собранная автоматически на основе профиля преподавателя.",
                                    "TEACHER", "APPROVED", ts, ts
                            });
                            totalSubjectCollections++;
                        }
                        subjectCollectionBookBatch.add(new Object[]{
                                teacherSubjectCollectionId, b.getId(), subjectCollectionBookBatch.size()
                        });
                    }
                    // VIEW — already in user_activity, nothing extra
                }
            }
        }

        // Bulk insert
        jdbc.batchUpdate(
                "INSERT INTO user_activity (id, user_id, book_id, activity_type, created_at) " +
                        "VALUES (?, ?, ?, ?, ?)",
                activityBatch);
        if (!reviewBatch.isEmpty()) {
            jdbc.batchUpdate(
                    "INSERT INTO reviews (id, book_id, user_id, rating, text, status, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (book_id, user_id) DO NOTHING",
                    reviewBatch);
        }
        if (!orderBatch.isEmpty()) {
            jdbc.batchUpdate(
                    "INSERT INTO orders (id, user_id, book_id, status, created_at) " +
                            "VALUES (?, ?, ?, ?, ?) ON CONFLICT (user_id, book_id) DO NOTHING",
                    orderBatch);
        }
        if (!collectionBatch.isEmpty()) {
            jdbc.batchUpdate(
                    "INSERT INTO collections (id, user_id, title, genre, description, status, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    collectionBatch);
            jdbc.batchUpdate(
                    "INSERT INTO collection_books (collection_id, book_id, position) " +
                            "VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                    collectionBookBatch);
        }
        if (!subjectCollectionBatch.isEmpty()) {
            jdbc.batchUpdate(
                    "INSERT INTO subject_collections (id, user_id, subject, specialty, specialty_name, " +
                            "title, description, author_role, status, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    subjectCollectionBatch);
            jdbc.batchUpdate(
                    "INSERT INTO subject_collection_books (subject_collection_id, book_id, position) " +
                            "VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                    subjectCollectionBookBatch);
        }

        double density = (double) totalActivities / numUsers / Math.max(books.size(), 1);
        long elapsed = System.currentTimeMillis() - start;

        SyntheticDataReport report = SyntheticDataReport.builder()
                .seed(props.getSynthetic().getSeed())
                .totalUsers(numUsers)
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .usersPerPersona(personaCounts)
                .totalActivities(totalActivities)
                .totalReviews(totalReviews)
                .totalOrders(totalOrders)
                .totalCollections(totalCollections)
                .totalCollectionBooks(totalCollectionBooks)
                .totalSubjectCollections(totalSubjectCollections)
                .interactionDensity(density)
                .elapsedMs(elapsed)
                .message("Сгенерировано успешно за " + elapsed + " мс")
                .build();

        log.info("Синтетика готова: users={} (студенты={}, преподаватели={}), activities={}, reviews={}, orders={}, density={}, elapsed={}мс",
                numUsers, totalStudents, totalTeachers, totalActivities, totalReviews, totalOrders,
                String.format(Locale.ROOT, "%.3f", density), elapsed);
        log.info("По персонам: {}", personaCounts);

        return report;
    }

    // =========================================================================
    // Sampling helpers
    // =========================================================================

    private PersonaProfile pickPersona(Random rng) {
        double r = rng.nextDouble();
        double cum = 0;
        for (int i = 0; i < PersonaCatalog.ALL.size(); i++) {
            cum += PersonaCatalog.DISTRIBUTION[i];
            if (r <= cum) return PersonaCatalog.ALL.get(i);
        }
        return PersonaCatalog.ALL.get(PersonaCatalog.ALL.size() - 1);
    }

    private double[] computeAffinities(PersonaProfile p, List<Book> books, Random rng) {
        double[] s = new double[books.size()];
        for (int i = 0; i < books.size(); i++) {
            s[i] = persoBookScore(p, books.get(i)) + rng.nextGaussian() * 0.05;
            if (s[i] < 0) s[i] = 0.001;
        }
        return s;
    }

    /** Returns persona×book affinity in [0..1] before noise. */
    static double persoBookScore(PersonaProfile p, Book b) {
        double genreScore = p.genreWeights().getOrDefault(b.getGenre(), 0.05);
        double authorBonus = 0.0;
        String author = b.getAuthor() == null ? "" : b.getAuthor();
        for (Map.Entry<String, Double> e : p.authorWeights().entrySet()) {
            if (author.contains(e.getKey())) {
                authorBonus = Math.max(authorBonus, e.getValue());
            }
        }
        return Math.min(1.0, genreScore + authorBonus);
    }

    private List<Book> sampleWithoutReplacement(List<Book> books, double[] scores, int n, Random rng) {
        record Indexed(int idx, double key) {}
        List<Indexed> keys = new ArrayList<>(books.size());
        for (int i = 0; i < books.size(); i++) {
            double w = Math.max(scores[i], 1e-6);
            double g = -Math.log(-Math.log(rng.nextDouble() + 1e-12) + 1e-12);
            keys.add(new Indexed(i, Math.log(w) + g));
        }
        keys.sort(Comparator.comparingDouble(Indexed::key).reversed());
        int take = Math.min(n, books.size());
        List<Book> out = new ArrayList<>(take);
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < take; i++) {
            int idx = keys.get(i).idx();
            if (seen.add(idx)) out.add(books.get(idx));
        }
        return out;
    }

    private int clampPoisson(Random rng, int mean, int min, int max) {
        double L = Math.exp(-mean);
        int k = 0;
        double p = 1.0;
        do {
            k++;
            p *= rng.nextDouble();
        } while (p > L);
        int result = k - 1;
        if (result < min) result = min;
        if (result > max) result = max;
        return result;
    }

    /**
     * For students: heavy VIEW, some ORDER + COLLECTION, few REVIEW.
     * For teachers: a SUBJECT_COLLECTION slot replaces some COLLECTION rolls
     * — they curate teaching materials.
     */
    private String pickActivityType(Random rng, boolean isTeacher) {
        double r = rng.nextDouble();
        if (isTeacher) {
            if (r < 0.45) return "VIEW";
            if (r < 0.62) return "ORDER";
            if (r < 0.78) return "COLLECTION";
            if (r < 0.92) return "SUBJECT_COLLECTION";
            return "REVIEW";
        }
        if (r < 0.50) return "VIEW";
        if (r < 0.75) return "ORDER";
        if (r < 0.90) return "COLLECTION";
        return "REVIEW";
    }

    private String pickTeacherSubject(PersonaProfile p, Random rng) {
        return switch (p.name()) {
            case "programmer"  -> "Введение в разработку программного обеспечения";
            case "ai_engineer" -> "Машинное обучение";
            case "security"    -> "Криптография и криптоанализ";
            case "humanitarian"-> "Философия";
            default            -> "Основы алгоритмизации и программирования";
        };
    }

    private OffsetDateTime randomPastDate(Random rng, int withinDays) {
        long secs = (long) (rng.nextDouble() * withinDays * 24 * 3600);
        return OffsetDateTime.now().minusSeconds(secs);
    }

    private int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private String generateReviewText(Book b, int rating, boolean isTeacher) {
        String base = switch (rating) {
            case 5 -> "Великолепная книга. Прочитал на одном дыхании, рекомендую каждому, кто интересуется этой темой. Автор раскрывает материал глубоко и интересно, многое заставляет задуматься.";
            case 4 -> "Хорошая книга, прочитал с удовольствием. Местами материал кажется затянутым, но в целом подача добротная и логичная. Рекомендую тем, кто только начинает погружаться в тему.";
            case 3 -> "Средняя книга. Есть полезные мысли, но в целом ничего выдающегося. Возможно, моё впечатление искажено — книгу читал давно и в неподходящее время.";
            case 2 -> "Книга не зашла. Слишком сухо, тяжеловесно, материал подан без огонька. Возможно, кому-то это и понравится, но мне не хватило живости и наглядных примеров.";
            default -> "Не моё совсем. Не смог продраться через первые главы — автор бесконечно повторяется, мысль ходит по кругу, аргументация слабая. Бросил на середине, возвращаться не планирую.";
        };
        String prefix = isTeacher
                ? "Рекомендую студентам как один из источников по теме. "
                : "";
        return String.format("«%s» — %s%s Размышляя над прочитанным, отмечаю, что автор последовательно раскрывает свои мысли и оставляет читателя с пищей для размышлений на дни вперёд.",
                b.getTitle(), prefix, base);
    }

    private record SyntheticUser(UUID userId, String login, PersonaProfile persona, boolean teacher) {}
}
