-- =====================================================================
-- V1: Initial schema — consolidated final state of all tables, columns,
-- and indexes used by the application.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---------------------------------------------------------------------
-- USERS  (students + teachers + moderators)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    login         VARCHAR(100) NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    patronymic    VARCHAR(100),
    faculty       VARCHAR(50),
    specialty     VARCHAR(255),
    course        VARCHAR(20),
    phone_number  VARCHAR(30),
    avatar_url    VARCHAR(500),
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    user_type     VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    department    VARCHAR(255),
    position      VARCHAR(255),
    is_blocked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- BOOKS  (with archive status + full-text search vector)
-- ---------------------------------------------------------------------
CREATE TABLE books (
    id               UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    title            VARCHAR(500) NOT NULL,
    author           VARCHAR(255) NOT NULL,
    genre            VARCHAR(100),
    year             INTEGER,
    description      TEXT,
    full_description TEXT,
    image_url        VARCHAR(500),
    cover_url        VARCHAR(500),
    pages            INTEGER,
    publisher        VARCHAR(255),
    publish_year     INTEGER,
    isbn             VARCHAR(20)  UNIQUE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    search_vector    TSVECTOR,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_books_search_vector ON books USING GIN(search_vector);
CREATE INDEX idx_books_status        ON books(status);

-- ---------------------------------------------------------------------
-- FACULTIES + SPECIALTIES  (admin-managed academic taxonomy)
-- ---------------------------------------------------------------------
CREATE TABLE faculties (
    id         UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    name       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE specialties (
    id         UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    faculty_id UUID         NOT NULL REFERENCES faculties(id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (faculty_id, name)
);

CREATE INDEX idx_specialties_faculty ON specialties(faculty_id);

-- ---------------------------------------------------------------------
-- SUBJECTS  (globally unique by name; M2M to specialties)
-- ---------------------------------------------------------------------
CREATE TABLE subjects (
    id         UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    name       VARCHAR(255) NOT NULL UNIQUE,
    is_common  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE subject_specialties (
    subject_id   UUID NOT NULL REFERENCES subjects(id)    ON DELETE CASCADE,
    specialty_id UUID NOT NULL REFERENCES specialties(id) ON DELETE CASCADE,
    PRIMARY KEY (subject_id, specialty_id)
);

CREATE INDEX idx_subject_specialties_specialty ON subject_specialties(specialty_id);

-- ---------------------------------------------------------------------
-- REVIEWS
-- ---------------------------------------------------------------------
CREATE TABLE reviews (
    id                UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    book_id           UUID         NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    user_id           UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating            INTEGER      NOT NULL CHECK (rating >= 1 AND rating <= 5),
    text              TEXT,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    moderator_comment TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (book_id, user_id)
);

CREATE INDEX idx_reviews_book_id ON reviews(book_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_status  ON reviews(status);

-- ---------------------------------------------------------------------
-- COLLECTIONS  (user-curated book lists)
-- ---------------------------------------------------------------------
CREATE TABLE collections (
    id                UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id           UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title             VARCHAR(255) NOT NULL,
    genre             VARCHAR(100),
    description       TEXT,
    status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    moderator_comment TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_collections_user_id ON collections(user_id);
CREATE INDEX idx_collections_status  ON collections(status);

CREATE TABLE collection_books (
    collection_id UUID    NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    book_id       UUID    NOT NULL REFERENCES books(id)       ON DELETE CASCADE,
    position      INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (collection_id, book_id)
);

-- ---------------------------------------------------------------------
-- SUBJECT COLLECTIONS  (teacher/student curated lists tied to a subject)
-- ---------------------------------------------------------------------
CREATE TABLE subject_collections (
    id                UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id           UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject           VARCHAR(255) NOT NULL,
    specialty         VARCHAR(255) NOT NULL,
    specialty_name    VARCHAR(255) NOT NULL,
    title             VARCHAR(255) NOT NULL,
    description       TEXT,
    author_role       VARCHAR(20),
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    moderator_comment TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE subject_collection_books (
    subject_collection_id UUID    NOT NULL REFERENCES subject_collections(id) ON DELETE CASCADE,
    book_id               UUID    NOT NULL REFERENCES books(id)               ON DELETE CASCADE,
    position              INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (subject_collection_id, book_id)
);

-- ---------------------------------------------------------------------
-- COMMENTS  (polymorphic — on review / collection / subject_collection)
-- ---------------------------------------------------------------------
CREATE TABLE comments (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id               UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    review_id             UUID REFERENCES reviews(id)             ON DELETE CASCADE,
    collection_id         UUID REFERENCES collections(id)         ON DELETE CASCADE,
    subject_collection_id UUID REFERENCES subject_collections(id) ON DELETE CASCADE,
    text                  TEXT NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT comments_parent_check CHECK (
        (review_id IS NOT NULL AND collection_id IS NULL AND subject_collection_id IS NULL) OR
        (review_id IS NULL AND collection_id IS NOT NULL AND subject_collection_id IS NULL) OR
        (review_id IS NULL AND collection_id IS NULL AND subject_collection_id IS NOT NULL)
    )
);

CREATE INDEX idx_comments_review_id             ON comments(review_id);
CREATE INDEX idx_comments_collection_id         ON comments(collection_id);
CREATE INDEX idx_comments_subject_collection_id ON comments(subject_collection_id);

-- ---------------------------------------------------------------------
-- LIKES  (polymorphic across REVIEW / COMMENT / COLLECTION / SUBJECT_COLLECTION)
-- ---------------------------------------------------------------------
CREATE TABLE likes (
    id          UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type VARCHAR(20)  NOT NULL,
    target_id   UUID         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, target_type, target_id)
);

CREATE INDEX idx_likes_target ON likes(target_type, target_id);
CREATE INDEX idx_likes_user   ON likes(user_id);

-- ---------------------------------------------------------------------
-- EVENTS  (book club / lectures with registration)
-- ---------------------------------------------------------------------
CREATE TABLE events (
    id               UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    date             DATE         NOT NULL,
    time             VARCHAR(10),
    location         VARCHAR(255),
    max_participants INTEGER,
    organizer        VARCHAR(255),
    created_by       UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_events_date ON events(date);

CREATE TABLE event_participants (
    event_id      UUID        NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id       UUID        NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, user_id)
);

-- ---------------------------------------------------------------------
-- ORDERS  (book reservation queue)
-- ---------------------------------------------------------------------
CREATE TABLE orders (
    id         UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id    UUID         NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, book_id)
);

CREATE INDEX idx_orders_user_id ON orders(user_id);

-- ---------------------------------------------------------------------
-- USER ACTIVITY  (event log for the recommendation engine)
-- ---------------------------------------------------------------------
CREATE TABLE user_activity (
    id            UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id       UUID         NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    activity_type VARCHAR(50)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_activities_user_id ON user_activity(user_id);

-- ---------------------------------------------------------------------
-- USER PREFERENCES  (cold-start fallback for the recommender)
-- ---------------------------------------------------------------------
CREATE TABLE user_preferences (
    id               UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id          UUID         NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    favorite_genres  TEXT[],
    favorite_authors TEXT[],
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
