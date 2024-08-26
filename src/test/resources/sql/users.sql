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
ADD CONSTRAINT ck_users_role CHECK (role in ('ADMIN', 'LIBRARIAN'));

INSERT INTO users (
  email
, hashedPassword
, firstName
, lastName
, company
, role
) VALUES (
  'dawid@dlakomy.github.io'
, '$2a$10$0l3cq8mOClq3ppBzkVLr0OdssC0BOv0rJrwqcf0JxAeydvHeT1Xhi'
, 'Dawid'
, 'Hungry'
, 'DL corp.'
, 'ADMIN'
);

INSERT INTO users (
  email
, hashedPassword
, firstName
, lastName
, company
, role
) VALUES (
  'john@lakomy.github.io'
, '$2a$10$3AN4sSQmbWXlkog6OIJjuesZ0cbi9uWd34j9Lx22Izv9faYD.H6qy'
, 'John'
, 'Hungrytoo'
, 'DL corp.'
, 'LIBRARIAN'
);