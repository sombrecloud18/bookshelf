-- Function to update search vector on books
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

-- Populate existing rows
UPDATE books SET search_vector =
    setweight(to_tsvector('russian', COALESCE(title, '')), 'A') ||
    setweight(to_tsvector('russian', COALESCE(author, '')), 'B') ||
    setweight(to_tsvector('russian', COALESCE(genre, '')), 'C') ||
    setweight(to_tsvector('russian', COALESCE(description, '')), 'D');
