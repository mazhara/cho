CREATE TABLE users (
  user_id bigserial NOT NULL UNIQUE
, username text NOT NULL
, email text NOT NULL
, hashedPassword text NOT NULL
, firstName text
, lastName text
, company text
, phone text
, data_processing_consent_at timestamptz
, role text NOT NULL
);

ALTER TABLE users
ADD CONSTRAINT pk_users PRIMARY KEY (email);

ALTER TABLE users
ADD CONSTRAINT ck_users_role CHECK (role in ('ADMIN', 'LIBRARIAN', 'READER'));

CREATE UNIQUE INDEX users_username_lower_idx ON users (LOWER(username));

INSERT INTO users (
  username
, email
, hashedPassword
, firstName
, lastName
, company
, role
) VALUES (
  'dawid'
, 'dawid@dlakomy.github.io'
, '$2a$10$0l3cq8mOClq3ppBzkVLr0OdssC0BOv0rJrwqcf0JxAeydvHeT1Xhi'
, 'Dawid'
, 'Hungry'
, 'DL corp.'
, 'ADMIN'
);

INSERT INTO users (
  username
, email
, hashedPassword
, firstName
, lastName
, company
, role
) VALUES (
  'john'
, 'john@lakomy.github.io'
, '$2a$10$3AN4sSQmbWXlkog6OIJjuesZ0cbi9uWd34j9Lx22Izv9faYD.H6qy'
, 'John'
, 'Hungrytoo'
, 'DL corp.'
, 'LIBRARIAN'
);

INSERT INTO users (
  username
, email
, hashedPassword
, firstName
, lastName
, company
, role
) VALUES (
  'reader'
, 'reader@library.test'
, '$2a$10$0l3cq8mOClq3ppBzkVLr0OdssC0BOv0rJrwqcf0JxAeydvHeT1Xhi'
, 'Alex'
, 'Reader'
, NULL
, 'READER'
);