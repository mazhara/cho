CREATE DATABASE library;
\c library;

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

CREATE TABLE users (
  email text NOT NULL
, hashedPassword text NOT NULL
, firstName text
, lastName text
, company text
, role text NOT NULL
);

ALTER TABLE users
ADD CONSTRAINT pk_users PRIMARY KEY (email);

ALTER TABLE users
ADD CONSTRAINT ck_users_role CHECK (role in ('ADMIN', 'RECRUITER'));

CREATE TABLE recoverytokens (
  email text NOT NULL,
  token text NOT NULL,
  expiration bigint NOT NULL
);

ALTER TABLE recoverytokens
ADD CONSTRAINT pk_recoverytokens PRIMARY KEY (email);