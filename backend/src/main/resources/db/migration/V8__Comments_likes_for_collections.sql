-- =====================================================================
-- V8: Comments and likes for collections.
--
-- 1) Расширяем таблицу comments полиморфной ссылкой на subject_collections,
--    чтобы можно было комментировать учебные подборки (раньше — только
--    рецензии и обычные подборки).
-- 2) Расширяем чек-констрейнт comments так, чтобы для каждого комментария
--    был ровно один parent (review / collection / subject_collection).
-- 3) Дополняем allow-list типов лайков (COLLECTION, SUBJECT_COLLECTION) —
--    раньше там были только REVIEW и COMMENT.
-- =====================================================================

ALTER TABLE comments
    ADD COLUMN IF NOT EXISTS subject_collection_id UUID REFERENCES subject_collections(id) ON DELETE CASCADE;

-- Снимаем старый «ровно один из двух» check, не угадывая его имя.
DO $$
DECLARE
    cname text;
BEGIN
    FOR cname IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE rel.relname = 'comments' AND con.contype = 'c'
    LOOP
        EXECUTE format('ALTER TABLE comments DROP CONSTRAINT %I', cname);
    END LOOP;
END $$;

ALTER TABLE comments
    ADD CONSTRAINT comments_parent_check CHECK (
        (review_id IS NOT NULL AND collection_id IS NULL AND subject_collection_id IS NULL) OR
        (review_id IS NULL AND collection_id IS NOT NULL AND subject_collection_id IS NULL) OR
        (review_id IS NULL AND collection_id IS NULL AND subject_collection_id IS NOT NULL)
    );

CREATE INDEX IF NOT EXISTS idx_comments_subject_collection_id ON comments(subject_collection_id);
CREATE INDEX IF NOT EXISTS idx_comments_collection_id ON comments(collection_id);
CREATE INDEX IF NOT EXISTS idx_comments_review_id ON comments(review_id);
