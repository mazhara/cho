CREATE TABLE books(
  id uuid DEFAULT gen_random_uuid()
, name text NOT NULL
, author text NOT NULL
, description text NOT NULL
, publisher text NOT NULL
, year
, inHallOnly boolean NOT NULL DEFAULT false
, tags text[]
, image text
);

ALTER TABLE books
ADD CONSTRAINT pk_books PRIMARY KEY (id);

INSERT INTO books(
    id
  , name
  , author
  , description
  , publisher
  , description
  , year
  , inHallOnly
  , tags
  , image
  ) VALUES (
    '843df718-ec6e-4d49-9289-f799c0f40064' -- id
  , 'Harry Potter' -- name
  , 'J. R' -- author
  , 'An awesome book' -- description
  , 'Penguin' -- publisher
  , 1998 -- year
  , false -- inHallOnly
  , ARRAY [ 'children', 'fantasy', 'english' ] -- tags
  , NULL -- image
  )