# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Frontend (`cd frontend`)
```bash
npm install          # Install dependencies
npm run dev          # Dev server at http://localhost:5173
npm run build        # Production build
npm run preview      # Preview production build
npm run lint         # Lint code
npm run format       # Format code (Prettier)
```

### Backend (`cd backend`)
```bash
mvn spring-boot:run                              # Start server at http://localhost:8088
mvn spring-boot:run -Dspring-boot.run.profiles=dev  # Run with dev profile
mvn clean install                                # Build and install
mvn test                                         # Run all tests
mvn test -Dtest=BookServiceTest                  # Run specific test
mvn clean package                                # Package for production
```

### Docker (full stack)
```bash
cp .env.example .env
docker-compose up -d    # Start all services (frontend :3000, backend :8088, db :5432)
docker-compose down
docker-compose logs -f
```

### Database migrations
Flyway runs automatically on startup. New migration files: `backend/src/main/resources/db/migration/V{N}__Description.sql`

## Architecture

This is a full-stack bookstore app. The frontend is a Vue 3 SPA; the backend is a Spring Boot REST API; PostgreSQL is the database.

### Backend (`backend/src/main/java/com/bookshelf/`)

Standard layered Spring Boot architecture:
- `controller/` — REST controllers, all routes prefixed with `/api`
- `service/` — Business logic
- `repository/` — Spring Data JPA repositories
- `entity/` — JPA entities (UUIDs as primary keys)
- `dto/` — Request/response DTOs (Java records)
- `security/` — `JwtTokenProvider` and `JwtAuthenticationFilter`
- `config/SecurityConfig.java` — Security rules and CORS config
- `exception/` — `AppException` (custom) + `GlobalExceptionHandler`

**Auth model**: Stateless JWT. Token stored client-side in `localStorage` under `bookshelf_auth_token`. Role stored under `bookshelf_auth_role`. Two roles: `USER` and `MODERATOR` (admin in the UI is `MODERATOR` in Spring Security).

**Key non-obvious detail**: The frontend refers to the admin role as `"admin"` in localStorage, but Spring Security uses `ROLE_MODERATOR`. Admin routes in the frontend check `role === 'admin'`, while backend `@PreAuthorize` uses `hasRole("MODERATOR")`.

**Recommendation engine** (`RecommendationService`): Uses collaborative filtering via `UserActivityRepository`, falling back to random unread books. Personal recommendations return three lists: `personal`, `popular`, `newBooks`.

### Frontend (`frontend/src/`)

- `pages/user/` — User-facing pages (thin wrappers that import from `pages/MainPage/`, etc.)
- `pages/admin/` — Admin/moderator pages
- `pages/auth/` — Login and registration
- `layouts/` — `AppLayout.vue` (user) and `AdminLayout.vue` (admin) with their sidebar/header components
- `stores/` — Pinia stores (currently `recommendationStore.js` and `subjectCollectionsStore.js` as composable functions, not `defineStore`)
- `router/index.js` — Route guard reads token/role from localStorage; redirects unauthenticated users to `/auth`
- `constants/` — `genreColors.js`, `studyData.js`
- `@` alias — resolves to `frontend/src/`

**UI library**: `@nuxt/ui` (registered as `ui` plugin in `main.ts`). Primary color is `blue`.

### Database

Three Flyway migrations:
- `V1__Initial_schema.sql` — Full schema
- `V2__Add_search_vector.sql` — Full-text search support
- `V3__Seed_initial_data.sql` — Seed data

`spring.jpa.hibernate.ddl-auto=validate` — schema is managed exclusively by Flyway in production.

### Default ports / env

| Service  | Default |
|----------|---------|
| Backend  | 8088 (env: `SERVER_PORT`) |
| Frontend dev | 5173 |
| Frontend Docker | 3000 |
| DB       | 5455 (env: `DB_URL`) |

Environment variables are documented in `.env.example`. JWT secret defaults are development-only; override via `JWT_SECRET` in production.