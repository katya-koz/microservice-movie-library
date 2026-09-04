CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- for gen_random_uuid()

CREATE TABLE IF NOT EXISTS upload_jobs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    media_type        TEXT NOT NULL
                          CHECK (media_type IN ('MOVIE', 'SHOW')),

    status            smallint DEFAULT 0,
                        --   CHECK (status IN ('PENDING', 'SAVING_FILES', 'SAVING_IMAGES', 'COMPLETED', 'FAILED')),

    progress_percent  INTEGER NOT NULL DEFAULT 0
                          CHECK (progress_percent BETWEEN 0 AND 100),

    tmdb_id           BIGINT NOT NULL,
    title   TEXT NOT NULL,

    -- no users table yet, so this just sits NULL for now
    requested_by      UUID,

    error_message     TEXT,
    catalog_payload   JSONB NOT NULL,

    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at      TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_upload_jobs_status     ON upload_jobs (status);
CREATE INDEX IF NOT EXISTS idx_upload_jobs_tmdb_id    ON upload_jobs (tmdb_id);
CREATE INDEX IF NOT EXISTS idx_upload_jobs_created_at ON upload_jobs (created_at DESC);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_upload_jobs_updated_at ON upload_jobs;
CREATE TRIGGER trg_upload_jobs_updated_at
    BEFORE UPDATE ON upload_jobs
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

