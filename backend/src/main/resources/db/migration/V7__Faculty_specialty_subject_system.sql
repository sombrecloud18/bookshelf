-- =====================================================================
-- V7: Faculty / Specialty / Subject system
--
-- Move the previously hardcoded study taxonomy (faculties, specialties)
-- into the database, and refactor subjects from "(specialty, name) is
-- unique" to "name is globally unique with a M2M link to specialties".
-- This enables sharing book collections across specialties that share
-- the same subject — per the product rules.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Faculties
-- ---------------------------------------------------------------------
CREATE TABLE faculties (
    id         UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    name       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- Specialties
-- A specialty NAME may repeat across faculties (rule from product spec)
-- but is unique WITHIN a single faculty.
-- ---------------------------------------------------------------------
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
-- Seed faculties + specialties from the previous frontend constants.
-- Keeps existing user profiles / collections that reference these
-- specialty names by string still working.
-- ---------------------------------------------------------------------
INSERT INTO faculties (name) VALUES
    ('ФКП'), ('ФКСИС'), ('ФИБ'), ('ФИТУ'), ('ФРЭ'), ('ВФ'), ('ИЭФ')
ON CONFLICT (name) DO NOTHING;

INSERT INTO specialties (faculty_id, name)
SELECT f.id, s.name
FROM faculties f
JOIN (VALUES
    ('ФКП',   'ПОИТ'),  ('ФКП',   'ВМСиС'), ('ФКП',   'КС'),
    ('ФКСИС', 'ИИ'),    ('ФКСИС', 'ПИ'),    ('ФКСИС', 'АСОИ'),
    ('ФИБ',   'ТК'),    ('ФИБ',   'ЗИ'),    ('ФИБ',   'МС'),
    ('ФИТУ',  'ИСиТ'),  ('ФИТУ',  'УПИ'),   ('ФИТУ',  'БИ'),
    ('ФРЭ',   'РЭиС'),  ('ФРЭ',   'МСиС'),  ('ФРЭ',   'ЭТ'),
    ('ВФ',    'ТТП'),   ('ВФ',    'ЭиУ'),   ('ВФ',    'МТ'),
    ('ИЭФ',   'МЭ'),    ('ИЭФ',   'ЭС'),    ('ИЭФ',   'УЭП')
) AS s(faculty_name, name) ON s.faculty_name = f.name
ON CONFLICT (faculty_id, name) DO NOTHING;

-- ---------------------------------------------------------------------
-- Subjects refactor.
--   * Drop UNIQUE(specialty, name) — replace with UNIQUE(name) globally.
--   * Add is_common flag (subject is offered to all specialties).
--   * Move (specialty, name) pairs into the new M2M table.
-- ---------------------------------------------------------------------
ALTER TABLE subjects ADD COLUMN is_common BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE subjects ADD COLUMN legacy_specialty VARCHAR(50);
UPDATE subjects SET legacy_specialty = specialty;

ALTER TABLE subjects DROP CONSTRAINT IF EXISTS subjects_specialty_name_key;
DROP INDEX IF EXISTS idx_subjects_specialty;

-- Deduplicate by name, keeping the earliest row.
WITH ranked AS (
    SELECT id,
           row_number() OVER (PARTITION BY name ORDER BY created_at, id) AS rn
    FROM subjects
)
DELETE FROM subjects WHERE id IN (SELECT id FROM ranked WHERE rn > 1);

ALTER TABLE subjects ADD CONSTRAINT subjects_name_unique UNIQUE (name);

CREATE TABLE subject_specialties (
    subject_id   UUID NOT NULL REFERENCES subjects(id)    ON DELETE CASCADE,
    specialty_id UUID NOT NULL REFERENCES specialties(id) ON DELETE CASCADE,
    PRIMARY KEY (subject_id, specialty_id)
);

CREATE INDEX idx_subject_specialties_specialty ON subject_specialties(specialty_id);

-- Re-link surviving subjects via the M2M table.
-- A subject keeps an entry for every legacy_specialty value that ever pointed at it
-- (we lost the duplicates above, but the surviving row is enough — collections were
-- referenced by name, not by row id).
INSERT INTO subject_specialties (subject_id, specialty_id)
SELECT s.id, sp.id
FROM subjects s
JOIN specialties sp ON sp.name = s.legacy_specialty
WHERE s.legacy_specialty IS NOT NULL
ON CONFLICT DO NOTHING;

-- Drop the now-redundant columns.
ALTER TABLE subjects DROP COLUMN specialty;
ALTER TABLE subjects DROP COLUMN legacy_specialty;
