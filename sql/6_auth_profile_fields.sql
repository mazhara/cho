\connect library

ALTER TABLE users ADD COLUMN IF NOT EXISTS phone TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS data_processing_consent_at TIMESTAMPTZ;
