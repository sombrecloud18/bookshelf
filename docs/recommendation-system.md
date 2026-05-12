# Гибридная рекомендательная система Bookshelf

> Техническое описание модуля `com.bookshelf.recommendation`.
> Для текста дипломной работы см. `docs/thesis-recommendation-chapter.md`.

## Архитектура

```
┌─────────────────────────────────────────────────────────────┐
│ Frontend: RecommendationWidget.vue → /api/recommendations   │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ Spring REST: RecommendationController                       │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ Orchestration: RecommendationService                        │
│   ↓                                                          │
│ HybridRecommendationService — выбор λ, нормализация, blend  │
│   ├── CollaborativeFilteringService  (item-kNN, cosine)     │
│   └── ContentBasedService            (TF-IDF + жанр/автор)  │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ Storage:                                                     │
│   • book_similarity   ← CosineItemSimilarityService (cron)  │
│   • book_tfidf        ← BookVectorService (cron + event)    │
│   • recommendation_log                                       │
│   • recommendation_evaluation                                │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ Synthetic data (dev only):                                   │
│   POST /api/admin/recommendation/seed?users=100             │
│   → SyntheticDataGenerator → 5 personas × ~25 interactions  │
└─────────────────────────────────────────────────────────────┘
```

## Алгоритмы

### Item-based Collaborative Filtering

Сходство книг считается косинусной мерой на матрице user×item:

```
sim(b₁, b₂) = Σᵤ rᵤ,b₁ · rᵤ,b₂ / (||r·,b₁||₂ · ||r·,b₂||₂)
```

Веса rating-сигналов (`recommendation.weights.*`):

| Сигнал | Вес |
|---|---|
| Approved review | rating × 1.0 (т.е. 1..5) |
| Книга в коллекции пользователя | 4.5 |
| Активная бронь | 4.0 |
| Просмотр (`user_activity.VIEW`) | 1.0 |

Скор кандидата для пользователя U:
```
score_CF(b) = Σ_{b' ∈ rated(U)}  sim(b, b') · rᵤ,b'
```

Топ-50 соседей сохраняется в `book_similarity` (пересчёт ночью + по требованию).

### Content-based filtering

Текст книги (title + author + genre + description + full_description) проходит через `org.apache.lucene.analysis.ru.RussianAnalyzer` — морфологический стеммер для русского языка. По полученным токенам считается TF-IDF, L2-нормализуется и сохраняется в `book_tfidf` (top-80 термов на книгу).

Профиль пользователя — взвешенная сумма векторов «понравившихся» книг (`rating ≥ 3`):
```
profile(U) = Σ rᵤ,b · vec(b) / Σ rᵤ,b
```

Финальный CB-скор:
```
score_CB(b) = α · cos(profile(U), vec(b))
            + β · I[b.genre ∈ favouriteGenres(U)]
            + γ · I[b.author contains favouriteAuthor(U)]
```
Веса: α=0.6, β=0.25, γ=0.15 (конфигурируется в `application.properties`).

### Гибридный оркестратор

Скоры обоих алгоритмов min-max нормализуются в [0..1] и смешиваются:
```
final(b) = λ · score_CF(b) + (1 - λ) · score_CB(b)
```

Параметр λ выбирается адаптивно по количеству позитивных взаимодействий:

| Количество взаимодействий | λ | Доминирующий алгоритм |
|---|---|---|
| < 3 (cold start) | 0.0 | CB + предпочтения |
| 3 – 10 (warm) | 0.3 | CB > CF |
| > 10 (hot) | 0.7 | CF > CB |

## Синтетические данные для оценки

Поскольку система ещё не имеет реальных пользователей, оценочные эксперименты проводятся на синтетическом наборе, сгенерированном **persona-based simulation**. Это стандартная методика cold-launch evaluation (Adomavicius & Tuzhilin, 2005; Burke, 2002).

5 архетипов с заданными предпочтениями:
1. **humanitarian** — Толстой, Достоевский, Булгаков (Роман, Поэма, Антиутопия)
2. **programmer** — Мартин, Макконнелл, Фаулер (Программирование, Базы данных)
3. **ai_engineer** — Жерон, Гудфеллоу, Бишоп (ИИ + смежное)
4. **security** — Шнайер, Митник, Форшоу (Безопасность + ОС)
5. **generalist** — смесь всего понемногу

Распределение: 25/25/20/15/15. По умолчанию N=100 пользователей × Poisson(25) взаимодействий + 8% шума.

## Команды

### Bootstrap пайплайна

```bash
# 1. Накатить миграции (V9 — схема, V10 — каталог)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 2. Залогиниться под admin/admin123 → получить JWT

# 3. Сгенерировать 100 синтетических пользователей
curl -X POST http://localhost:8088/api/admin/recommendation/seed \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"users": 100}'

# 4. Пересчитать матрицы (на старте сделается само, но можно форсировать)
curl -X POST http://localhost:8088/api/admin/recommendation/rebuild-cf \
     -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:8088/api/admin/recommendation/rebuild-cb \
     -H "Authorization: Bearer $TOKEN"

# 5. Запустить оценку
curl -X POST http://localhost:8088/api/admin/recommendation/evaluate \
     -H "Authorization: Bearer $TOKEN"
```

### Smoke-проверки

```sql
-- Размер матриц
SELECT COUNT(*) FROM book_similarity;     -- ожидаем ~2-3 тыс строк
SELECT COUNT(*) FROM book_tfidf;          -- ~3-4 тыс строк
SELECT COUNT(*) FROM users WHERE login LIKE 'synth_%';  -- ожидаем N

-- Глазной тест сходства
SELECT b1.title, b2.title, ROUND(similarity::numeric, 3)
  FROM book_similarity bs
  JOIN books b1 ON bs.book_id_1 = b1.id
  JOIN books b2 ON bs.book_id_2 = b2.id
 WHERE b1.title = 'Чистый код'
 ORDER BY similarity DESC LIMIT 5;
-- ожидаем: Совершенный код, Алгоритмы, Грокаем алгоритмы, ...

-- Топ-термы книги по TF-IDF
SELECT term, ROUND(weight::numeric, 3) AS w
  FROM book_tfidf
 WHERE book_id = (SELECT id FROM books WHERE title='Глубокое обучение')
 ORDER BY weight DESC LIMIT 10;
-- ожидаем: «обучен», «нейрон», «сет», «глубок» и т.п. (стеммированы)

-- Метрики evaluation
SELECT algorithm, metric_name, ROUND(metric_value::numeric, 3) AS v
  FROM recommendation_evaluation
 WHERE run_id = (SELECT run_id FROM recommendation_evaluation ORDER BY computed_at DESC LIMIT 1)
 ORDER BY algorithm, metric_name;
```

## Конфигурация

Все параметры в `application.properties`, префикс `recommendation.*`. Перезагрузка не требуется только для веса; новые `top-k-neighbors` или `min-common-users` применяются после ребилда матриц.

## Структура классов

```
com.bookshelf.config
  RecommendationProperties              — все настройки

com.bookshelf.recommendation
  data/
    PersonaProfile                      — POJO архетипа
    PersonaCatalog                      — 5 статических персон
    SyntheticDataGenerator              — генератор
    SyntheticDataReport                 — отчёт

  cf/
    InteractionMatrix                   — sparse user×book
    InteractionMatrixBuilder            — собирает из reviews/orders/collections/views
    CosineItemSimilarityService         — пересчёт book_similarity
    CollaborativeFilteringService       — scoreForUser, similarTo
    ScoredBook                          — record (bookId, score)
    SimilarityRefreshScheduler          — ночной + startup-trigger

  cb/
    RussianTextVectorizer               — Lucene RussianAnalyzer wrapper
    TfIdfCalculator                     — корпусной TF-IDF + cosine helpers
    BookVectorService                   — пересчёт book_tfidf
    ContentBasedService                 — scoreForUser

  hybrid/
    HybridRecommendationService         — λ-blending, normalize, log

  eval/
    EvaluationService                   — train/test split + 5 алгоритмов
    EvaluationReport                    — DTO результата

com.bookshelf.controller.admin
  RecommendationAdminController         — /seed, /rebuild-cf, /rebuild-cb, /evaluate

com.bookshelf.service
  RecommendationService                 — публичный orchestrator (переписан)
```
