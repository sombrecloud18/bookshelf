# Полное руководство по развёртыванию Bookshelf

Этот документ описывает развёртывание проекта с нуля на чистом ноутбуке: от установки Docker до работающего UI с активной рекомендательной системой и сгенерированными тестовыми данными.

После выполнения всех шагов у вас будет:
- 🐘 PostgreSQL с накатанными миграциями V1..V10 (включая схему рекомендательной системы)
- 🌱 100 синтетических пользователей с реалистичными взаимодействиями
- 🤖 Пересчитанные матрицы CF (item-similarity) и CB (TF-IDF)
- 🌐 UI на http://localhost:3000 с работающими персональными рекомендациями
- 🔑 Аккаунт администратора `admin` / `admin123` и 100 тестовых аккаунтов `synth_user_001` .. `synth_user_100` (пароль у всех `admin123`)

---

## 1. Системные требования

| Компонент | Минимум | Рекомендуется |
|---|---|---|
| RAM | 4 ГБ свободно | 8 ГБ |
| Диск | 5 ГБ свободно | 10 ГБ |
| ОС | Windows 10 21H2+, macOS 12+, Ubuntu 20.04+ | актуальная |
| CPU | 2 ядра | 4 ядра |

**Все необходимые компоненты (PostgreSQL, Java, Maven, Node.js) запускаются внутри Docker** — устанавливать их в систему не нужно. На хосте достаточно:
- Docker Desktop (Windows/macOS) или Docker Engine + Compose plugin (Linux)
- Git
- Любой HTTP-клиент для пары curl-команд (curl уже идёт в Windows 10+ и в любом Linux/macOS)

---

## 2. Шаг 1 — Установка Docker

### Windows

1. Откройте https://www.docker.com/products/docker-desktop/
2. Скачайте **Docker Desktop for Windows** → запустите установщик
3. Во время установки оставьте галочку **«Use WSL 2 instead of Hyper-V»**
4. После установки перезагрузите компьютер
5. Запустите Docker Desktop. Дождитесь зелёного индикатора в нижнем левом углу: **«Engine running»**

Проверка в PowerShell или Git Bash:
```bash
docker --version
docker compose version
```
Должно вывести что-то вроде:
```
Docker version 27.x.x, build ...
Docker Compose version v2.x.x
```

### macOS

1. Скачайте **Docker Desktop for Mac** с https://www.docker.com/products/docker-desktop/ (выберите Apple Silicon или Intel)
2. Перетащите Docker.app в Applications, запустите
3. Дождитесь иконки в menu bar

Проверка в Terminal:
```bash
docker --version && docker compose version
```

### Linux (Ubuntu/Debian)

```bash
# Удалить старые версии (если есть)
sudo apt-get remove docker docker-engine docker.io containerd runc

# Установить
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Добавить пользователя в группу docker (чтобы не вызывать sudo каждый раз)
sudo usermod -aG docker $USER
newgrp docker

# Проверка
docker --version && docker compose version
```

---

## 3. Шаг 2 — Установка Git

### Windows
Скачайте https://git-scm.com/download/win → установите с настройками по умолчанию.

### macOS
```bash
xcode-select --install
```
(Или установите через Homebrew: `brew install git`)

### Linux
```bash
sudo apt-get install -y git
```

Проверка:
```bash
git --version
```

---

## 4. Шаг 3 — Клонирование репозитория

Откройте терминал (PowerShell / Git Bash / Terminal) в директории, где вы хотите разместить проект (например, `~/Projects` или `C:\Users\<вы>\IdeaProjects`):

```bash
git clone https://github.com/sombrecloud18/bookshelf.git
cd bookshelf
```

Проверка, что вы на актуальной ветке:
```bash
git status
git log --oneline -5
```

Если нужна конкретная ветка/коммит, переключитесь:
```bash
git checkout main
git pull
```

---

## 5. Шаг 4 — Настройка `.env`

В корне репозитория есть `.env.example`. Скопируйте его и при желании отредактируйте:

```bash
# Linux / macOS / Git Bash
cp .env.example .env

# Windows PowerShell
Copy-Item .env.example .env
```

Содержимое `.env` достаточно для запуска из коробки. **Для production обязательно поменяйте** `JWT_SECRET` на длинную случайную строку.

> ℹ️ Порты, на которых будут видны сервисы на хосте, прописаны прямо в `docker-compose.yml`:
> - **DB** — 5455 (PostgreSQL)
> - **Backend** — 8088 (Spring Boot)
> - **Frontend** — 3000 (Nginx со статикой Vue)

Если какие-то из этих портов у вас заняты, поправьте секцию `ports:` в `docker-compose.yml` перед сборкой.

---

## 6. Шаг 5 — Сборка образов и запуск контейнеров

### Первый запуск (build + start)

```bash
docker compose up -d --build
```

Что произойдёт:
1. **Pull базовых образов** (postgres, eclipse-temurin, node, nginx) — 1–3 минуты в зависимости от скорости интернета
2. **Сборка backend**: Maven скачает все зависимости (включая Lucene), скомпилирует и упакует jar — 3–7 минут на первом запуске
3. **Сборка frontend**: `npm ci` + `vite build` — 1–3 минуты
4. **Старт контейнеров**: db → backend (после healthcheck'а БД) → frontend

Дождитесь сообщения вида:
```
✔ Container bookshelf-db        Healthy
✔ Container bookshelf-backend   Healthy
✔ Container bookshelf-frontend  Started
```

Если хочется наблюдать процесс в реальном времени без `-d`:
```bash
docker compose up --build
```
(но тогда придётся не закрывать терминал; для повседневной работы используйте `-d`)

### Проверка статуса

```bash
docker compose ps
```

Ожидаемый вывод:
```
NAME                 STATUS         PORTS
bookshelf-db         Up (healthy)   0.0.0.0:5455->5432/tcp
bookshelf-backend    Up (healthy)   0.0.0.0:8088->8088/tcp
bookshelf-frontend   Up (healthy)   0.0.0.0:3000->3000/tcp
```

Если статус `Up (unhealthy)` или `Restarting` — переходите к разделу **Troubleshooting** в конце документа.

---

## 7. Шаг 6 — Проверка миграций БД

Flyway применяет миграции автоматически при старте backend. Убедимся:

```bash
docker compose logs backend | grep -i flyway
```

Ожидаемый фрагмент:
```
Flyway Community Edition 9.x.x by Redgate
Successfully validated 10 migrations
Migrating schema "public" to version "9 - Recommendation engine schema"
Migrating schema "public" to version "10 - Catalog expansion for personas"
Successfully applied 10 migrations to schema "public", now at version v10
```

Если миграций накатилось меньше — что-то не так со схемой. Самая частая причина — старая база в volume от предыдущей версии проекта. Решается в Troubleshooting → «Полная очистка».

Подключиться к БД вручную можно так:
```bash
docker exec -it bookshelf-db psql -U bookshelf_user -d bookshelf
```
Внутри psql:
```sql
\dt
SELECT version, description FROM flyway_schema_history ORDER BY installed_rank;
\q
```

---

## 8. Шаг 7 — Сразу после старта (что уже работает)

На этом этапе вы уже можете открыть http://localhost:3000 и зайти как `admin` / `admin123`. Но **рекомендации будут пустыми**, потому что в системе только справочные данные из сидов V3/V5/V10 (≈47 книг, ≈5 живых пользователей-сидов, ≈7 рецензий — слишком разрежённо для CF).

Поэтому переходим к инициализации синтетики.

---

## 9. Шаг 8 — Генерация синтетических данных

Все админ-команды работают через REST API под токеном модератора. Получаем токен:

### 9.1. Получение JWT-токена

**Git Bash / Linux / macOS:**
```bash
TOKEN=$(curl -s -X POST http://localhost:8088/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"admin123"}' \
  | sed 's/.*"token":"\([^"]*\)".*/\1/')

echo "TOKEN length: ${#TOKEN}"
```
Ожидается `TOKEN length: 200+`. Если длина 0 — backend не отдаёт логин (см. Troubleshooting).

**Windows PowerShell:**
```powershell
$response = Invoke-RestMethod -Uri http://localhost:8088/api/auth/login `
    -Method Post -ContentType "application/json" `
    -Body '{"login":"admin","password":"admin123"}'
$TOKEN = $response.token
Write-Host "TOKEN length: $($TOKEN.Length)"
```

### 9.2. Запуск генератора (100 пользователей)

**Git Bash / Linux / macOS:**
```bash
curl -s -X POST http://localhost:8088/api/admin/recommendation/seed \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"users":100}'
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri http://localhost:8088/api/admin/recommendation/seed `
    -Method Post `
    -Headers @{Authorization = "Bearer $TOKEN"} `
    -ContentType "application/json" `
    -Body '{"users":100}' | ConvertTo-Json
```

Ожидаемый ответ (числа могут немного отличаться из-за зерна RNG — `seed=42` фиксирован, так что должны быть точно такие):
```json
{
  "seed": 42,
  "totalUsers": 100,
  "usersPerPersona": {
    "security": 16,
    "generalist": 18,
    "programmer": 23,
    "ai_engineer": 26,
    "humanitarian": 17
  },
  "totalActivities": 2541,
  "totalReviews": 216,
  "totalOrders": 664,
  "totalCollections": 98,
  "totalCollectionBooks": 381,
  "interactionDensity": 0.5406,
  "elapsedMs": 343,
  "message": "Сгенерировано успешно за 343 мс"
}
```

**Возможные числа пользователей:** от 1 до 1000. Стандарт — 100, для дипломной демки этого хватит с запасом.

### 9.3. Пересчёт матриц на свежих данных

После генерации синтетики **обязательно** перестраиваем item-similarity и TF-IDF — иначе CF/CB будут опираться на пустую матрицу, собранную при старте на пустых таблицах.

```bash
# CF — item-item cosine similarity
curl -s -X POST http://localhost:8088/api/admin/recommendation/rebuild-cf \
  -H "Authorization: Bearer $TOKEN"

# CB — TF-IDF векторы книг
curl -s -X POST http://localhost:8088/api/admin/recommendation/rebuild-cb \
  -H "Authorization: Bearer $TOKEN"
```

Ожидается:
```json
{"rows":2162}   // book_similarity
{"rows":1130}   // book_tfidf
```

> 💡 Ночное расписание (`recommendation.cf.refresh-cron=0 0 3 * * *`) запускает этот пересчёт автоматически. Принудительный rebuild нужен только сразу после большой загрузки данных.

---

## 10. Шаг 9 — Smoke-проверки качества

Убедимся, что алгоритмы выдают осмысленные результаты.

### 10.1. CF — похожие книги

Подключаемся к БД и проверяем, что у «Чистого кода» в соседях стоят технические книги:
```bash
docker exec bookshelf-db psql -U bookshelf_user -d bookshelf -c "
SELECT b1.title AS book, b2.title AS similar_to, ROUND(similarity::numeric, 3) AS sim
  FROM book_similarity bs
  JOIN books b1 ON bs.book_id_1 = b1.id
  JOIN books b2 ON bs.book_id_2 = b2.id
 WHERE b1.title = 'Чистый код'
 ORDER BY similarity DESC LIMIT 5;
"
```
Ожидается список из «Рефакторинг», «Spring в действии», «Алгоритмы», «Эффективная Java», «Совершенный код».

То же для классики:
```bash
docker exec bookshelf-db psql -U bookshelf_user -d bookshelf -c "
SELECT b2.title, ROUND(similarity::numeric, 3) AS sim
  FROM book_similarity bs
  JOIN books b1 ON bs.book_id_1 = b1.id
  JOIN books b2 ON bs.book_id_2 = b2.id
 WHERE b1.title = 'Мастер и Маргарита'
 ORDER BY similarity DESC LIMIT 5;
"
```
Ожидается: «1984», «Маленький принц», «Анна Каренина», «Три товарища», «Доктор Живаго».

### 10.2. CB — топ-термы TF-IDF

```bash
docker exec bookshelf-db psql -U bookshelf_user -d bookshelf -c "
SELECT term, ROUND(weight::numeric, 3) AS w
  FROM book_tfidf
 WHERE book_id = (SELECT id FROM books WHERE title = 'Глубокое обучение')
 ORDER BY weight DESC LIMIT 10;
"
```
Ожидается стеммированные термы: `обучен`, `глубок`, `учебник`, `рекуррентн`, `сверточн`, `нейрон`.

### 10.3. End-to-end рекомендации

Залогиньтесь под синтетическим пользователем и получите его рекомендации:
```bash
SYNTH_TOKEN=$(curl -s -X POST http://localhost:8088/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"synth_user_001","password":"admin123"}' \
  | sed 's/.*"token":"\([^"]*\)".*/\1/')

curl -s -X GET http://localhost:8088/api/recommendations \
  -H "Authorization: Bearer $SYNTH_TOKEN"
```

В ответе должен быть массив `personal` с `matchScore` 0.5–0.95 и `source: "HYBRID"` для большинства карточек.

---

## 11. Шаг 10 — Проверка в браузере

Откройте http://localhost:3000

### Сценарий A — Администратор
1. Логин: `admin` / `admin123`
2. После входа вас редиректит в `/admin` (админка)

### Сценарий B — Активный пользователь
1. Логин: `synth_user_001` / `admin123`
2. Главная → вкладка **«Рекомендации»**
3. Должны увидеть:
   - Подзаголовок «Подобрано гибридным алгоритмом ИИ»
   - 10 карточек в секции **«Рекомендации для вас»** с цветными процентными бейджами в левом верхнем углу (зелёный 80%+, синий 60–79%, жёлтый 40–59%)
   - Секции **«Популярное»** и **«Новинки»** ниже

### Сценарий C — Холодный пользователь
1. Зарегистрируйте новый аккаунт через UI (любой login)
2. Вкладка «Рекомендации» — сначала будет CTA «Заполните предпочтения»
3. Перейдите в аккаунт → задайте любимые жанры/авторов → вернитесь
4. Появятся CB-рекомендации (`source: "CB"`)

---

## 12. Шаг 11 — Опционально: offline-evaluation для дипломной

Запускает все пять алгоритмов (Random / Popular / CF / CB / Hybrid) на train/test split 80/20 и сохраняет метрики в БД:

```bash
curl -s -X POST http://localhost:8088/api/admin/recommendation/evaluate \
  -H "Authorization: Bearer $TOKEN"
```

Результат (числа примерные):
```json
{
  "runId": "...",
  "topK": 10,
  "trainRatio": 0.8,
  "trainSize": 956,
  "testSize": 240,
  "usersEvaluated": 100,
  "metrics": [
    {"algorithm": "RANDOM",  "values": {"precision@K": 0.05, ...}},
    {"algorithm": "POPULAR", "values": {"precision@K": 0.18, ...}},
    {"algorithm": "CF",      "values": {"precision@K": 0.42, ...}},
    {"algorithm": "CB",      "values": {"precision@K": 0.35, ...}},
    {"algorithm": "HYBRID",  "values": {"precision@K": 0.48, ...}}
  ]
}
```

Метрики также сохраняются в таблице `recommendation_evaluation` — удобно для вставки в пояснительную записку.

> ⚠️ Evaluation временно прячет 20% позитивных сигналов и пересчитывает матрицы на train-данных, потом восстанавливает. Прогон занимает 30–90 секунд. На время прогона `/api/recommendations` для других пользователей выдаст устаревшие данные — не запускайте под нагрузкой.

---

## 13. Полезные команды повседневной работы

```bash
# Посмотреть логи backend в реальном времени
docker compose logs -f backend

# Перезапустить только backend (после изменения кода)
docker compose up -d --build backend

# Зайти в БД psql
docker exec -it bookshelf-db psql -U bookshelf_user -d bookshelf

# Полная остановка (контейнеры остановятся, данные в volumes останутся)
docker compose stop

# Запустить обратно (без сборки)
docker compose start

# Снести всё, включая данные (ОПАСНО — потеряете БД и все настройки)
docker compose down -v
```

### Управление синтетикой

```bash
# Удалить всех синтетических пользователей (login LIKE 'synth_%')
curl -X DELETE http://localhost:8088/api/admin/recommendation/seed \
  -H "Authorization: Bearer $TOKEN"

# Перегенерировать с другим количеством
curl -X POST http://localhost:8088/api/admin/recommendation/seed \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"users":200}'

# Не забыть пересчитать матрицы после перегенерации
curl -X POST http://localhost:8088/api/admin/recommendation/rebuild-cf \
  -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:8088/api/admin/recommendation/rebuild-cb \
  -H "Authorization: Bearer $TOKEN"
```

---

## 14. Troubleshooting

### «Port already in use»

Один из портов (3000 / 5455 / 8088) занят другим процессом.

**Поиск процесса:**
```bash
# Linux / macOS
lsof -i :8088

# Windows PowerShell
Get-NetTCPConnection -LocalPort 8088
```

**Решение:**
- Остановить мешающий процесс, либо
- В `docker-compose.yml` поменять левую часть портов (например, `8089:8088`), затем `docker compose up -d --build`

### Backend в статусе `unhealthy`

```bash
docker compose logs backend | tail -100
```

Самые частые причины:
- **БД ещё не готова** — должно автоматически восстановиться через `depends_on: service_healthy`. Если нет — `docker compose restart backend`
- **Старая БД с несовместимой схемой**: `Validate failed: Migration checksum mismatch` → нужна полная очистка (см. ниже)
- **Нехватка памяти JVM** → увеличить `JAVA_OPTS` в `docker-compose.yml` (по умолчанию `-Xmx512m`)

### Frontend показывает «Network error»

```bash
docker compose logs frontend
curl -i http://localhost:8088/api/health
```

- Если health-чек 200 OK — фронтенд просто не подцепился. Перезапустите: `docker compose restart frontend`
- Если health-чек не отвечает — backend не запустился, см. выше

### Миграции упали или не накатились

```bash
docker exec bookshelf-db psql -U bookshelf_user -d bookshelf \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

Если есть строка с `success = f` — Flyway требует ручной починки. Самое простое — полная очистка БД (данные потеряете):
```bash
docker compose down -v
docker compose up -d --build
```

### Полная переустановка с нуля

⚠️ **Удалит ВСЕ данные**, включая все ваши рецензии, аккаунты и т.д.

```bash
docker compose down -v --rmi local
docker volume prune -f
git pull
docker compose up -d --build
```

После этого пройдите шаги 8 и 9 заново.

### Логи в одном месте

```bash
docker compose logs > bookshelf-logs.txt
```

Файл можно приложить в багрепорт.

---

## 15. Список рабочих URL-адресов

| Сервис | URL | Назначение |
|---|---|---|
| Frontend (Web) | http://localhost:3000 | Пользовательский интерфейс |
| Backend (API) | http://localhost:8088/api | REST API |
| Healthcheck | http://localhost:8088/api/health | Проверка доступности backend |
| Книги | http://localhost:8088/api/books | Каталог |
| Рекомендации | http://localhost:8088/api/recommendations | Персональные (требует auth) |
| Admin / Seed | http://localhost:8088/api/admin/recommendation/seed | POST — генератор (MODERATOR) |
| Admin / Evaluate | http://localhost:8088/api/admin/recommendation/evaluate | POST — метрики (MODERATOR) |

---

## 16. Чек-лист для защиты дипломной

Перед демо на ГЭК:

- [ ] Контейнеры запущены и `healthy`
- [ ] Миграции дошли до **v10**
- [ ] Сгенерирована синтетика на **100 пользователей**
- [ ] Пересчитаны **book_similarity** и **book_tfidf**
- [ ] Прогнан `evaluate` — есть свежий run_id в `recommendation_evaluation`
- [ ] Подготовлены 2 демо-аккаунта: `synth_user_001` (активный) и новый (для cold-start)
- [ ] Сделаны скриншоты с бейджами процентов
- [ ] Есть готовая глава для записки: `docs/thesis-recommendation-chapter.md`

---

## 17. Дополнительная литература

- 📖 **Техническая документация рекомендательной системы:** [`docs/recommendation-system.md`](recommendation-system.md)
- 📖 **Глава для пояснительной записки:** [`docs/thesis-recommendation-chapter.md`](thesis-recommendation-chapter.md)
- 📖 **Гайд для разработчиков (CLAUDE.md):** [`../CLAUDE.md`](../CLAUDE.md)

---

> 💬 Если что-то не работает по этому гайду — пришлите вывод `docker compose ps`, `docker compose logs backend | tail -50` и описание шага, на котором застряли.
