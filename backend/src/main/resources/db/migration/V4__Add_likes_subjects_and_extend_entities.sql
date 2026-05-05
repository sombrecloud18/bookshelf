-- =====================================================================
-- V4: Likes, Subjects, archive flag for books, moderator_comment for collections,
--      teacher fields for users.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Users: teacher / student differentiation
-- ---------------------------------------------------------------------
ALTER TABLE users
    ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    ADD COLUMN department VARCHAR(255),
    ADD COLUMN position   VARCHAR(255);

-- Existing admin keeps user_type='STUDENT' but role='MODERATOR' — that's fine.

-- ---------------------------------------------------------------------
-- Books: soft-archive status
-- ---------------------------------------------------------------------
ALTER TABLE books
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX idx_books_status ON books(status);

-- ---------------------------------------------------------------------
-- Collections: moderator comment for rejection feedback
-- ---------------------------------------------------------------------
ALTER TABLE collections
    ADD COLUMN moderator_comment TEXT;

-- ---------------------------------------------------------------------
-- Subjects: master list of academic subjects per specialty
-- ---------------------------------------------------------------------
CREATE TABLE subjects (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    specialty  VARCHAR(50)  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (specialty, name)
);

CREATE INDEX idx_subjects_specialty ON subjects(specialty);

-- ---------------------------------------------------------------------
-- Likes: polymorphic likes for reviews and comments
-- ---------------------------------------------------------------------
CREATE TABLE likes (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type VARCHAR(20)  NOT NULL,  -- 'REVIEW' | 'COMMENT'
    target_id   UUID         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, target_type, target_id)
);

CREATE INDEX idx_likes_target  ON likes(target_type, target_id);
CREATE INDEX idx_likes_user    ON likes(user_id);

-- Drop the old denormalised counters now that the new system replaces them.
ALTER TABLE reviews  DROP COLUMN likes;
ALTER TABLE comments DROP COLUMN likes;
