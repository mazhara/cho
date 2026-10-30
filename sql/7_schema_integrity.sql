\connect library

-- Books without an ISBN use NULL instead of the '-' placeholder.
UPDATE Books SET isbn = NULL WHERE isbn = '-' OR btrim(isbn) = '';
CREATE UNIQUE INDEX IF NOT EXISTS uq_books_isbn ON Books (isbn) WHERE isbn IS NOT NULL;

-- Foreign keys that were left commented out in 0_init.sql.
DELETE FROM Book_Copies WHERE book_id IS NULL OR book_id NOT IN (SELECT book_id FROM Books);
UPDATE Books SET publisher_id = NULL
WHERE publisher_id IS NOT NULL AND publisher_id NOT IN (SELECT publisher_id FROM Publishers);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_books_publisher') THEN
        ALTER TABLE Books ADD CONSTRAINT fk_books_publisher
            FOREIGN KEY (publisher_id) REFERENCES Publishers(publisher_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_book_copies_book') THEN
        ALTER TABLE Book_Copies ADD CONSTRAINT fk_book_copies_book
            FOREIGN KEY (book_id) REFERENCES Books(book_id) ON DELETE CASCADE;
    END IF;
END $$;
ALTER TABLE Book_Copies ALTER COLUMN book_id SET NOT NULL;

-- exemplar_number is a library-wide inventory number: renumber duplicates, then enforce uniqueness.
UPDATE Book_Copies bc SET exemplar_number = nextval(pg_get_serial_sequence('book_copies', 'exemplar_number'))
WHERE pg_get_serial_sequence('book_copies', 'exemplar_number') IS NOT NULL
  AND EXISTS (SELECT 1 FROM Book_Copies o
              WHERE o.exemplar_number = bc.exemplar_number AND o.ctid < bc.ctid);
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_book_copies_exemplar_number') THEN
        ALTER TABLE Book_Copies ADD CONSTRAINT uq_book_copies_exemplar_number UNIQUE (exemplar_number);
    END IF;
END $$;

-- Book languages: Ukrainian, English and French.
CREATE TABLE IF NOT EXISTS Languages (
    language_id SERIAL PRIMARY KEY,
    language_name VARCHAR(100) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS BookLanguages (
    book_id UUID,
    language_id INT,
    PRIMARY KEY (book_id, language_id),
    FOREIGN KEY (book_id) REFERENCES Books(book_id) ON DELETE CASCADE,
    FOREIGN KEY (language_id) REFERENCES Languages(language_id)
);
INSERT INTO Languages (language_name) VALUES ('Ukrainian'), ('English'), ('French')
ON CONFLICT (language_name) DO NOTHING;
