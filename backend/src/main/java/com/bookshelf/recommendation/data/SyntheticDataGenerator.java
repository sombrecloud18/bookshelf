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

    private static final String[] FACULTIES = { "ФКП", "ФКСИС", "ФИБ", "ФИТУ" };
    private static final String[] SPECIALTIES = { "ПОИТ", "ИИ", "ЗИ", "ВМСиС", "ИСиТ", "ПИ" };
    private static final String[] COURSES = { "1 курс", "2 курс", "3 курс", "4 курс" };
    private static final String[] FIRST_NAMES = {
            "Александр", "Андрей", "Дмитрий", "Иван", "Михаил", "Сергей", "Артём",
            "Анна", "Мария", "Екатерина", "Ольга", "Юлия", "Наталья", "Виктория"
    };
    private static final String[] LAST_NAMES = {
            "Иванов", "Петров", "Сидоров", "Кузнецов", "Смирнов", "Попов",
            "Васильев", "Михайлов", "Новиков", "Фёдоров", "Морозов", "Волков"
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

        Map<String, Integer> personaCounts = new HashMap<>();
        int totalActivities = 0;
        int totalReviews = 0;
        int totalOrders = 0;
        int totalCollections = 0;
        int totalCollectionBooks = 0;

        List<Object[]> userBatch = new ArrayList<>();

        // Pre-generate users (persona, id, login) so we can batch-insert
        List<SyntheticUser> users = new ArrayList<>(numUsers);
        for (int i = 0; i < numUsers; i++) {
            PersonaProfile persona = pickPersona(rng);
            personaCounts.merge(persona.name(), 1, Integer::sum);
            UUID uid = UUID.randomUUID();
            String login = String.format("%suser_%03d", SYNTH_LOGIN_PREFIX, i + 1);
            String email = String.format(Locale.ROOT, "%s@bookshelf.dev", login);
            String first = FIRST_NAMES[rng.nextInt(FIRST_NAMES.length)];
            String last = LAST_NAMES[rng.nextInt(LAST_NAMES.length)];
            String faculty = FACULTIES[rng.nextInt(FACULTIES.length)];
            String specialty = SPECIALTIES[rng.nextInt(SPECIALTIES.length)];
            String course = COURSES[rng.nextInt(COURSES.length)];

            userBatch.add(new Object[]{
                    uid, login, email, passwordHash, first, last, null,
                    faculty, specialty, course, null, "USER", "STUDENT", false
            });
            users.add(new SyntheticUser(uid, login, persona));
        }

        jdbc.batchUpdate(
                "INSERT INTO users (id, login, email, password_hash, first_name, last_name, patronymic, " +
                        "faculty, specialty, course, phone_number, role, user_type, is_blocked) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                userBatch);

        // user_preferences uses TEXT[] columns — JdbcTemplate.batchUpdate(Object[])
        // can't auto-convert String[] to a java.sql.Array, so we open a single
        // connection and bind arrays via Connection.createArrayOf(...).
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

        for (SyntheticUser u : users) {
            // Sample interaction count from clipped Poisson
            int interactions = clampPoisson(rng, props.getSynthetic().getInteractionsMean(),
                    props.getSynthetic().getInteractionsMin(), props.getSynthetic().getInteractionsMax());

            // Build affinity scores for every book, then sample without replacement
            double[] scores = computeAffinities(u.persona(), books, rng);
            List<Book> selected = sampleWithoutReplacement(books, scores, interactions, rng);

            // Inject some noise interactions (uncorrelated with persona) so the matrix
            // is not perfectly block-diagonal
            int noiseCount = (int) Math.round(interactions * props.getSynthetic().getNoiseRatio());
            for (int k = 0; k < noiseCount; k++) {
                Book noise = books.get(rng.nextInt(books.size()));
                if (!selected.contains(noise)) selected.add(noise);
            }

            UUID userCollectionId = null;  // lazily created if any COLLECTION-type signal

            for (Book b : selected) {
                String type = pickActivityType(rng);
                double affinity = persoBookScore(u.persona(), b);
                OffsetDateTime when = randomPastDate(rng, 60);
                Timestamp ts = Timestamp.from(when.toInstant());

                // Always record into user_activity (this is what CF matrix builder reads)
                activityBatch.add(new Object[]{ UUID.randomUUID(), u.userId(), b.getId(), type, ts });
                totalActivities++;

                switch (type) {
                    case "REVIEW" -> {
                        int rating = clamp((int) Math.round(1 + affinity * 4 + rng.nextGaussian() * 0.4), 1, 5);
                        reviewBatch.add(new Object[]{
                                UUID.randomUUID(), b.getId(), u.userId(),
                                rating, generateReviewText(b, rating),
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
                    // VIEW — already in user_activity
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

        double density = (double) totalActivities / numUsers / Math.max(books.size(), 1);
        long elapsed = System.currentTimeMillis() - start;

        SyntheticDataReport report = SyntheticDataReport.builder()
                .seed(props.getSynthetic().getSeed())
                .totalUsers(numUsers)
                .usersPerPersona(personaCounts)
                .totalActivities(totalActivities)
                .totalReviews(totalReviews)
                .totalOrders(totalOrders)
                .totalCollections(totalCollections)
                .totalCollectionBooks(totalCollectionBooks)
                .interactionDensity(density)
                .elapsedMs(elapsed)
                .message("Сгенерировано успешно за " + elapsed + " мс")
                .build();

        log.info("Синтетика готова: users={}, activities={}, reviews={}, orders={}, density={}, elapsed={}мс",
                numUsers, totalActivities, totalReviews, totalOrders,
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
        // Gumbel-max trick for weighted sampling without replacement
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
        // Knuth's algorithm
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

    private String pickActivityType(Random rng) {
        double r = rng.nextDouble();
        if (r < 0.50) return "VIEW";
        if (r < 0.75) return "ORDER";
        if (r < 0.90) return "COLLECTION";
        return "REVIEW";
    }

    private OffsetDateTime randomPastDate(Random rng, int withinDays) {
        long secs = (long) (rng.nextDouble() * withinDays * 24 * 3600);
        return OffsetDateTime.now().minusSeconds(secs);
    }

    private int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private String generateReviewText(Book b, int rating) {
        // Минимум 200 символов — соответствует валидации отзывов.
        String base = switch (rating) {
            case 5 -> "Великолепная книга. Прочитал на одном дыхании, рекомендую каждому, кто интересуется этой темой. Автор раскрывает материал глубоко и интересно, многое заставляет задуматься.";
            case 4 -> "Хорошая книга, прочитал с удовольствием. Местами материал кажется затянутым, но в целом подача добротная и логичная. Рекомендую тем, кто только начинает погружаться в тему.";
            case 3 -> "Средняя книга. Есть полезные мысли, но в целом ничего выдающегося. Возможно, моё впечатление искажено — книгу читал давно и в неподходящее время.";
            case 2 -> "Книга не зашла. Слишком сухо, тяжеловесно, материал подан без огонька. Возможно, кому-то это и понравится, но мне не хватило живости и наглядных примеров.";
            default -> "Не моё совсем. Не смог продраться через первые главы — автор бесконечно повторяется, мысль ходит по кругу, аргументация слабая. Бросил на середине, возвращаться не планирую.";
        };
        return String.format("«%s» — %s Размышляя над прочитанным, отмечаю, что автор последовательно раскрывает свои мысли и оставляет читателя с пищей для размышлений на дни вперёд.",
                b.getTitle(), base);
    }

    private record SyntheticUser(UUID userId, String login, PersonaProfile persona) {}
}
