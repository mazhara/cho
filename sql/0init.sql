CREATE DATABASE library;
\c library;

CREATE TYPE author_type AS ENUM ('Author', 'Composer', 'Editor', 'Illustrator');

CREATE TABLE Publishers (
    publisher_id SERIAL PRIMARY KEY,
    publisher_name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    website VARCHAR(255),
    contact_email VARCHAR(100)
); 


CREATE TABLE Books(
  book_id UUID DEFAULT gen_random_uuid()
, isbn VARCHAR(20)
, title text NOT NULL
, description text 
, publisher_id INT
, genre VARCHAR(100)
, published_year integer
, tags text[]
, cover text
-- , FOREIGN KEY (publisher_id) REFERENCES Publishers(publisher_id)
);

ALTER TABLE Books
ADD CONSTRAINT pk_books PRIMARY KEY (book_id);


CREATE TABLE Book_Copies (
    copy_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id UUID,
    exemplar_number SERIAL,
    available BOOLEAN DEFAULT TRUE,
    in_library_only BOOLEAN DEFAULT FALSE
--     FOREIGN KEY (book_id) REFERENCES Books(book_id) ON DELETE CASCADE
);


CREATE TABLE Languages (
    language_id SERIAL PRIMARY KEY,
    language_name VARCHAR(100) NOT NULL
);


CREATE TABLE BookLanguages (
    book_id UUID,
    language_id INT,
    PRIMARY KEY (book_id, language_id),
    FOREIGN KEY (book_id) REFERENCES Books(book_id) ON DELETE CASCADE,
    FOREIGN KEY (language_id) REFERENCES Languages(language_id)
);


CREATE TABLE Authors (
    author_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(1000),
    last_name VARCHAR(1000),
    author_type author_type
);

CREATE TABLE BookAuthors (
    book_id UUID,
    author_id UUID,
    PRIMARY KEY (book_id, author_id),
    FOREIGN KEY (book_id) REFERENCES Books(book_id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES Authors(author_id) ON DELETE CASCADE
);


