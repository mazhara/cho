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
, image text
, FOREIGN KEY (publisher_id) REFERENCES Publishers(publisher_id)
);

ALTER TABLE Books
ADD CONSTRAINT pk_books PRIMARY KEY (book_id);


CREATE TABLE Book_Copies (
    copy_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id UUID,
    exemplar_number SERIAL,
    available BOOLEAN DEFAULT TRUE,
    in_library_only BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (book_id) REFERENCES Books(book_id) ON DELETE CASCADE
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
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL
);

CREATE TABLE BookAuthors (
    book_id UUID,
    author_id UUID,
    PRIMARY KEY (book_id, author_id),
    FOREIGN KEY (book_id) REFERENCES Books(book_id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES Authors(author_id) ON DELETE CASCADE
);

INSERT INTO Languages (language_name) VALUES
('English'),
('French'),
('Spanish');

INSERT INTO Publishers (
  publisher_name
  , address
  , website
  , contact_email
  ) VALUES (
    'Broom Publish'
    , '123 Magic Street, London, UK'
    , 'https://broompublish.com'
    , 'contact@broompublish.com'
  );

INSERT INTO Books (
  book_id
  , isbn
  , title
  , description
  , publisher_id
  , genre
  , published_year
  , tags
  , image
  ) VALUES (
     '843df718-ec6e-4d49-9289-f799c0f40064' -- id
    ,'978-3-16-148410-0'
    , 'Harry Potter and the Philosopher Stone'
    , 'A young wizard discovers his magical heritage.'
    , 1
    , 'Fantasy'
    , 1997
    , ARRAY['fantasy', 'magic', 'children']
    , NULL
  );

INSERT INTO Authors (
   author_id
  , first_name
  , last_name
  ) VALUES (
    '90bd1ae7-14db-4b36-ac9e-fa1e23c65d1a',
    'J.K.'
    , 'Rowling');

INSERT INTO BookAuthors (book_id, author_id) VALUES
((SELECT book_id FROM Books WHERE title = 'Harry Potter and the Philosopher Stone'),
 (SELECT author_id FROM Authors WHERE last_name = 'Rowling'));

INSERT INTO Book_Copies (copy_id, book_id, exemplar_number, available, in_library_only) VALUES
('90bd1ae7-14db-4b36-ac9e-fa1e23c65d1a', (SELECT book_id FROM Books WHERE title = 'Harry Potter and the Philosopher Stone'), DEFAULT, TRUE, FALSE);

INSERT INTO BookLanguages (book_id, language_id) VALUES
((SELECT book_id FROM Books WHERE title = 'Harry Potter and the Philosopher Stone'),
 (SELECT language_id FROM Languages WHERE language_name = 'English'));
