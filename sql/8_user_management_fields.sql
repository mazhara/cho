\connect library

ALTER TABLE users ADD COLUMN IF NOT EXISTS user_id BIGSERIAL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS username TEXT;

UPDATE users
SET username = split_part(email, '@', 1)
WHERE username IS NULL OR BTRIM(username) = '';

WITH duplicates AS (
    SELECT user_id, username,
           ROW_NUMBER() OVER (PARTITION BY LOWER(username) ORDER BY user_id) AS duplicate_number
    FROM users
)
UPDATE users
SET username = users.username || users.user_id
FROM duplicates
WHERE users.user_id = duplicates.user_id
  AND duplicates.duplicate_number > 1;

ALTER TABLE users ALTER COLUMN username SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS users_user_id_idx ON users (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS users_username_lower_idx ON users (LOWER(username));
