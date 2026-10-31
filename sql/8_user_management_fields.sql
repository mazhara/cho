\connect library

BEGIN;
LOCK TABLE users IN ACCESS EXCLUSIVE MODE;

ALTER TABLE users ADD COLUMN IF NOT EXISTS user_id BIGSERIAL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS username TEXT;

UPDATE users
SET username = split_part(email, '@', 1)
WHERE username IS NULL OR BTRIM(username) = '';

DO $$
DECLARE
    duplicate RECORD;
    candidate TEXT;
    suffix BIGINT;
BEGIN
    FOR duplicate IN
        SELECT user_id, username
        FROM (
            SELECT user_id, username,
                   ROW_NUMBER() OVER (PARTITION BY LOWER(username) ORDER BY user_id) AS duplicate_number
            FROM users
        ) ranked
        WHERE duplicate_number > 1
        ORDER BY user_id
    LOOP
        candidate := duplicate.username || '_' || duplicate.user_id;
        suffix := 0;
        WHILE EXISTS (SELECT 1 FROM users WHERE LOWER(username) = LOWER(candidate)) LOOP
            suffix := suffix + 1;
            candidate := duplicate.username || '_' || duplicate.user_id || '_' || suffix;
        END LOOP;
        UPDATE users SET username = candidate WHERE user_id = duplicate.user_id;
    END LOOP;
END $$;

ALTER TABLE users ALTER COLUMN username SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS users_user_id_idx ON users (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS users_username_lower_idx ON users (LOWER(username));

COMMIT;
