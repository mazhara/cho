CREATE TABLE books(
  id uuid DEFAULT gen_random_uuid()
, name text NOT NULL
, author text NOT NULL
, description text NOT NULL
, publisher text NOT NULL
, year integer
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
  , year
  , inHallOnly
  , tags
  , image
  ) VALUES (
    '843df718-ec6e-4d49-9289-f799c0f40064' -- id
  , 'Harry Potter' -- name
  , 'JKR' -- author
  , 'An awesome book' -- description
  , 'broom publish' -- publisher
  , 1991 -- year
  , false -- inHallOnly
  , ARRAY [ 'fantasy', 'bestseller', 'children' ] -- tags
  , NULL -- image
  )