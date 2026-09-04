-- ============================================================
-- Movie / Catalog Service — PostgreSQL Schema
-- Handles movies, shows (with seasons/episodes), and the file
-- locations pointing to the actual media on disk.
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- for gen_random_uuid()

-- ============================================================
-- MOVIES
-- ============================================================
CREATE TABLE movies (
                        id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        tmdb_id          INTEGER UNIQUE,           -- link back to TMDB/Metadata service
                        title  TEXT NOT NULL,            -- e.g. "Interstellar"
                        release_date     DATE,
                        overview         TEXT,
                        poster_path      TEXT, -- local
                        backdrop_path    TEXT, -- local
                        tmdb_poster_path      TEXT,
                       tmdb_backdrop_path      TEXT,
                        runtime_minutes  INTEGER,
                        creator_names		TEXT, -- seperated by commas
                        created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                        updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_movies_title ON movies (title);

-- ============================================================
-- SHOWS
-- ============================================================
CREATE TABLE shows (
                       id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       tmdb_id          INTEGER UNIQUE,
                       title  TEXT NOT NULL,            -- e.g. "Breaking Bad"
                       first_air_date   DATE,
                       overview         TEXT,
                       poster_path      TEXT,
                       creator_names		TEXT, -- seperated by commas
                       backdrop_path    TEXT,
                       tmdb_poster_path      TEXT,
                       tmdb_backdrop_path      TEXT,
                       status           TEXT,                     -- Returning Series, Ended, Canceled...
                       created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_shows_title ON shows (title);

-- ============================================================
-- SEASONS
-- ============================================================
CREATE TABLE seasons (
                         id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         show_id          UUID NOT NULL REFERENCES shows(id) ON DELETE CASCADE,
                         season_number    INTEGER NOT NULL,         -- 0 = specials, matches TMDB convention
                         tmdb_season_id   INTEGER,
                         overview         TEXT,
                         poster_path      TEXT,
                         tmdb_poster_path      TEXT,
                         air_date         DATE,
                         created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

                         UNIQUE (show_id, season_number)
);

CREATE INDEX idx_seasons_show_id ON seasons (show_id);

-- ============================================================
-- EPISODES
-- ============================================================
CREATE TABLE episodes (
                          id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          season_id        UUID NOT NULL REFERENCES seasons(id) ON DELETE CASCADE,
                          episode_number   INTEGER NOT NULL,
                          tmdb_episode_id  INTEGER,
                          title            TEXT,
                          overview         TEXT,
                          air_date         DATE,
                          runtime_minutes  INTEGER,
                          tmdb_still_path      TEXT,
                          still_path       TEXT,                     -- episode thumbnail from TMDB
                          created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

                          UNIQUE (season_id, episode_number)
);

CREATE INDEX idx_episodes_season_id ON episodes (season_id);

-- ============================================================
-- MEDIA FILES
-- The actual playable file on disk. Belongs to exactly one
-- movie OR one episode — never both, never neither.
-- ============================================================
CREATE TABLE media_files (
                             id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             movie_id            UUID REFERENCES movies(id) ON DELETE CASCADE,
                             episode_id          UUID REFERENCES episodes(id) ON DELETE CASCADE,

                             file_path           TEXT NOT NULL UNIQUE,      -- canonical location on disk
                             canonical_filename  TEXT NOT NULL,             -- standardized name, e.g.
    -- "Breaking.Bad.S01E01.mkv"
                             original_filename   TEXT,                      -- as originally uploaded

                             container_format    TEXT,                      -- mkv, mp4, ...
                             video_codec         TEXT,
                             audio_codec         TEXT,
                             resolution          TEXT,                      -- e.g. "1920x1080"
                             duration_seconds    INTEGER,
                             file_size_bytes     BIGINT,

                             encoded_at          TIMESTAMPTZ,
                             created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

                             CONSTRAINT media_file_exactly_one_owner CHECK (
                                 (movie_id IS NOT NULL AND episode_id IS NULL) OR
                                 (movie_id IS NULL AND episode_id IS NOT NULL)
                                 )
);

CREATE INDEX idx_media_files_movie_id ON media_files (movie_id);
CREATE INDEX idx_media_files_episode_id ON media_files (episode_id);

-- ============================================================
-- SUBTITLES
-- One or more subtitle tracks per media file — different
-- languages, forced narrative subs, SDH/hearing-impaired
-- variants. Extracted to WebVTT sidecar files during encoding
-- so browsers can play them via <track kind="subtitles">.
-- ============================================================
CREATE TABLE subtitles (
                           id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           media_file_id          UUID NOT NULL REFERENCES media_files(id) ON DELETE CASCADE,

                           file_path              TEXT NOT NULL UNIQUE,       -- location of the .vtt sidecar
                           language_code          TEXT NOT NULL,               -- ISO 639-1, e.g. 'en', 'es'
                           label                  TEXT,                        -- e.g. "English (SDH)"
                           format                 TEXT NOT NULL DEFAULT 'vtt',  -- vtt, srt, ass

                           is_default             BOOLEAN NOT NULL DEFAULT false,

                           source                 TEXT NOT NULL DEFAULT 'embedded', -- embedded, external, downloaded
                           original_stream_index  INTEGER,                     -- ffprobe stream index, for traceability
                           created_at             TIMESTAMPTZ NOT NULL DEFAULT now()


);

CREATE INDEX idx_subtitles_media_file_id ON subtitles (media_file_id);

-- At most one default subtitle track per media file
CREATE UNIQUE INDEX one_default_subtitle_per_file
    ON subtitles (media_file_id)
    WHERE is_default;