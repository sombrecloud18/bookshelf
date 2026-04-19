-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================
-- USERS
-- =====================
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    login         VARCHAR(100) NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100),
    last_name     VARCHAR(100),
    patronymic    VARCHAR(100),
    faculty       VARCHAR(255),
    specialty     VARCHAR(255),
    course        VARCHAR(50),
    phone_number  VARCHAR(50),
    avatar_url    TEXT,
    role          VARCHAR(50)  NOT NULL DEFAULT 'USER',
    is_blocked    BOOLEAN      NOT NULL DEFAULT false,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- =====================
-- BOOKS
-- =====================
CREATE TABLE books (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title            VARCHAR(255) NOT NULL,
    author           VARCHAR(255) NOT NULL,
    genre            VARCHAR(100),
    year             INTEGER,
    description      TEXT,
    full_description TEXT,
    image_url        TEXT,
    cover_url        TEXT,
    pages            INTEGER,
    publisher        VARCHAR(255),
    publish_year     INTEGER,
    isbn             VARCHAR(30),
    search_vector    TSVECTOR,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =====================
-- REVIEWS
-- =====================
CREATE TABLE reviews (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    book_id    UUID         NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating     INTEGER      NOT NULL CHECK (rating >= 1 AND rating <= 5),
    text       TEXT,
    status     VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    likes      INTEGER      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (book_id, user_id)
);

-- =====================
-- COLLECTIONS (user-created)
-- =====================
CREATE TABLE collections (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    genre       VARCHAR(100),
    description TEXT,
    status      VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE collection_books (
    collection_id UUID    NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    book_id       UUID    NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    position      INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (collection_id, book_id)
);

-- =====================
-- SUBJECT COLLECTIONS (library/teacher collections)
-- =====================
CREATE TABLE subject_collections (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id          UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject          VARCHAR(255) NOT NULL,
    specialty        VARCHAR(255) NOT NULL,
    specialty_name   VARCHAR(255) NOT NULL,
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    author_role      VARCHAR(100),
    status           VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    moderator_comment TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE subject_collection_books (
    subject_collection_id UUID    NOT NULL REFERENCES subject_collections(id) ON DELETE CASCADE,
    book_id               UUID    NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    position              INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (subject_collection_id, book_id)
);

-- =====================
-- COMMENTS (polymorphic: on review or collection)
-- =====================
CREATE TABLE comments (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    review_id     UUID REFERENCES reviews(id) ON DELETE CASCADE,
    collection_id UUID REFERENCES collections(id) ON DELETE CASCADE,
    text          TEXT NOT NULL,
    likes         INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (
        (review_id IS NOT NULL AND collection_id IS NULL) OR
        (review_id IS NULL AND collection_id IS NOT NULL)
    )
);

-- =====================
-- EVENTS
-- =====================
CREATE TABLE events (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
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

CREATE TABLE event_participants (
    event_id      UUID        NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, user_id)
);

-- =====================
-- ORDERS (reading list / book requests)
-- =====================
CREATE TABLE orders (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id    UUID        NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    status     VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, book_id)
);

-- =====================
-- USER ACTIVITY (for recommendations)
-- =====================
CREATE TABLE user_activity (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id       UUID        NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =====================
-- USER PREFERENCES
-- =====================
CREATE TABLE user_preferences (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id          UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    favorite_genres  TEXT[],
    favorite_authors TEXT[],
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =====================
-- INDEXES
-- =====================
CREATE INDEX idx_books_search_vector ON books USING GIN(search_vector);
CREATE INDEX idx_reviews_book_id     ON reviews(book_id);
CREATE INDEX idx_reviews_user_id     ON reviews(user_id);
CREATE INDEX idx_reviews_status      ON reviews(status);
CREATE INDEX idx_collections_user_id ON collections(user_id);
CREATE INDEX idx_collections_status  ON collections(status);
CREATE INDEX idx_events_date         ON events(date);
CREATE INDEX idx_orders_user_id      ON orders(user_id);
CREATE INDEX idx_activities_user_id  ON user_activity(user_id);
