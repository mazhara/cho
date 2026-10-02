\connect library

CREATE TABLE IF NOT EXISTS events (
    event_id UUID PRIMARY KEY,
    title TEXT NOT NULL,
    event_date TIMESTAMPTZ NOT NULL,
    location TEXT NOT NULL,
    afishe_url TEXT,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    is_offline BOOLEAN NOT NULL DEFAULT FALSE,
    language TEXT NOT NULL,
    description TEXT NOT NULL,
    offline_address TEXT,
    registration_url TEXT
);

ALTER TABLE events ADD COLUMN IF NOT EXISTS registration_url TEXT;
ALTER TABLE events ADD COLUMN IF NOT EXISTS is_offline BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE events
SET is_offline = offline_address IS NOT NULL
WHERE is_offline = FALSE;
