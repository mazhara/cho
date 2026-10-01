\connect library

ALTER TABLE Books
ADD COLUMN IF NOT EXISTS catalog_added_at TIMESTAMPTZ;
