\connect library

CREATE TABLE IF NOT EXISTS users (
                                     email TEXT NOT NULL,
                                     hashedPassword TEXT NOT NULL,
                                     firstName TEXT,
                                     lastName TEXT,
                                     company TEXT,
                                     role TEXT NOT NULL DEFAULT 'READER',
                                     CONSTRAINT pk_users PRIMARY KEY (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'LIBRARIAN', 'READER'))
    );

ALTER TABLE users ADD COLUMN IF NOT EXISTS hashedPassword TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS firstName TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS lastName TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS company TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS role TEXT DEFAULT 'READER';
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'READER';
UPDATE users SET role = 'READER' WHERE role IS NULL;
ALTER TABLE users ALTER COLUMN role SET NOT NULL;

ALTER TABLE users DROP CONSTRAINT IF EXISTS ck_users_role;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users
    ADD CONSTRAINT ck_users_role
        CHECK (role IN ('ADMIN', 'LIBRARIAN', 'READER'));
