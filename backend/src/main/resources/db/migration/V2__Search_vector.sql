-- =====================================================================
-- V2: Full-text search vector trigger for the books table.
-- Russian stemmer + weighted fields (title > author > genre > description).
-- =====================================================================

CREATE OR REPLACE FUNCTION books_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('russian', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('russian', COALESCE(NEW.author, '')), 'B') ||
        setweight(to_tsvector('russian', COALESCE(NEW.genre, '')), 'C') ||
        setweight(to_tsvector('russian', COALESCE(NEW.description, '')), 'D');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER books_search_vector_trigger
    BEFORE INSERT OR UPDATE ON books
    FOR EACH ROW EXECUTE FUNCTION books_search_vector_update();
