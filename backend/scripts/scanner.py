#!/usr/bin/env python3

"""
scan_library.py

Scan a media library and populate the catalog database.

Expected layout:

    library/
    ├── .env
    ├── assets/
    ├── movies/
    │   └── Mulholland Drive (2002)/
    │       └── movie.mp4
    │
    └── shows/
        └── The Sopranos (1999)/
            ├── Season 1/
            │   ├── episode1.mp4
            │   ├── episode2.mp4
            │   └── episode1.srt
            │
            └── Season 2/
                ├── episode1.mp4
                ├── episode2.mp4
                └── episode1.srt

The directory name is used to search TMDB.

Movies:
    "<Title> (<Year>)"

Shows:
    "<Title> (<Year>)"

Episodes are numbered according to natural filename order.

Example:

    episode1.mp4
    episode2.mp4
    episode10.mp4

becomes:

    S01EP01.mp4
    S01EP02.mp4
    S01EP03.mp4

Subtitles are renamed along with their episode:

    episode1.srt -> S01EP01.en.srt

TMDB metadata and artwork are downloaded automatically.

Configuration is loaded from .env.
"""

from __future__ import annotations

import json
import logging
import os
import re
import shutil
import time
from dataclasses import dataclass
from datetime import date
from pathlib import Path
from typing import Optional

import requests
import psycopg2
from dotenv import load_dotenv


# ============================================================================
# Configuration
# ============================================================================

load_dotenv()

TMDB_API_BASE = "https://api.themoviedb.org/3"
TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/original"

TMDB_API_KEY = os.getenv("TMDB_API_KEY")

POSTGRES_HOST = os.getenv("POSTGRES_HOST", "localhost")
POSTGRES_PORT = os.getenv("POSTGRES_PORT", "5432")
POSTGRES_DB = os.getenv("POSTGRES_DB", "catalogdb")
POSTGRES_USER = os.getenv("POSTGRES_USER")
POSTGRES_PASSWORD = os.getenv("POSTGRES_PASSWORD")

VIDEO_EXTENSIONS = {
    ".mp4",
    ".mkv",
    ".mov",
    ".avi",
    ".webm",
    ".m4v",
    ".ts",
}

SUBTITLE_EXTENSIONS = {
    ".srt",
    ".vtt",
    ".ass",
    ".ssa",
    ".sub",
}

TITLE_YEAR_PATTERN = re.compile(
    r"^(?P<title>.+?)\s*\((?P<year>\d{4})\)\s*$"
)

SEASON_PATTERN = re.compile(
    r"season[\s_-]*0*(\d+)",
    re.IGNORECASE,
)

LANGUAGE_CODES = {
    "en": "English",
    "es": "Spanish",
    "fr": "French",
    "de": "German",
    "it": "Italian",
    "pt": "Portuguese",
    "nl": "Dutch",
    "ru": "Russian",
    "ja": "Japanese",
    "zh": "Chinese",
    "ko": "Korean",
    "ar": "Arabic",
    "hi": "Hindi",
    "tr": "Turkish",
    "pl": "Polish",
    "sv": "Swedish",
    "no": "Norwegian",
    "da": "Danish",
    "fi": "Finnish",
    "cs": "Czech",
    "el": "Greek",
    "he": "Hebrew",
}

logger = logging.getLogger("scan_library")


# ============================================================================
# Data classes
# ============================================================================

@dataclass
class EpisodeFile:
    season_number: int
    episode_number: int
    video_path: Path
    subtitles: list[Path]


# ============================================================================
# Utility functions
# ============================================================================

def natural_sort_key(path: Path):
    """
    Sort:

        episode1
        episode2
        episode10

    instead of:

        episode1
        episode10
        episode2
    """

    return [
        int(value) if value.isdigit() else value.lower()
        for value in re.split(r"(\d+)", path.name)
    ]


def parse_title_year(name: str):
    """
    Convert:

        Mulholland Drive (2002)

    into:

        ("Mulholland Drive", 2002)
    """

    match = TITLE_YEAR_PATTERN.match(name.strip())

    if not match:
        return name.strip(), None

    return (
        match.group("title").strip(),
        int(match.group("year")),
    )


def parse_date(value: Optional[str]) -> Optional[date]:
    if not value:
        return None

    try:
        return date.fromisoformat(value)
    except ValueError:
        return None


def find_video_files(directory: Path) -> list[Path]:
    return sorted(
        [
            p
            for p in directory.iterdir()
            if p.is_file()
            and p.suffix.lower() in VIDEO_EXTENSIONS
        ],
        key=natural_sort_key,
    )


def find_subtitle_files(directory: Path) -> list[Path]:
    return sorted(
        [
            p
            for p in directory.iterdir()
            if p.is_file()
            and p.suffix.lower() in SUBTITLE_EXTENSIONS
        ],
        key=natural_sort_key,
    )


def find_season_directories(show_dir: Path):
    seasons = []

    for directory in show_dir.iterdir():

        if not directory.is_dir():
            continue

        match = SEASON_PATTERN.search(directory.name)

        if not match:
            logger.warning(
                "Ignoring directory that is not a season: %s",
                directory,
            )
            continue

        season_number = int(match.group(1))

        seasons.append(
            (season_number, directory)
        )

    return sorted(
        seasons,
        key=lambda value: value[0],
    )


# ============================================================================
# Subtitle handling
# ============================================================================

def guess_subtitle_language(path: Path):
    """
    Look for a language token in the filename.

    Examples:

        episode1.en.srt -> en
        episode1.fr.srt -> fr
        episode1.srt    -> en (guessed)
    """

    tokens = re.split(
        r"[.\-_ ]+",
        path.stem.lower(),
    )

    for token in tokens:
        if token in LANGUAGE_CODES:
            return token, False

    return "en", True


def subtitle_matches_video(
    video: Path,
    subtitle: Path,
) -> bool:

    video_stem = video.stem.lower()
    subtitle_stem = subtitle.stem.lower()

    return (
        subtitle_stem == video_stem
        or subtitle_stem in video_stem
        or video_stem in subtitle_stem
    )


def find_subtitles_for_video(
    video: Path,
    subtitles: list[Path],
) -> list[Path]:

    return [
        subtitle
        for subtitle in subtitles
        if subtitle_matches_video(video, subtitle)
    ]


# ============================================================================
# Episode discovery
# ============================================================================

def build_episodes(
    season_number: int,
    season_dir: Path,
) -> list[EpisodeFile]:

    videos = find_video_files(season_dir)
    subtitles = find_subtitle_files(season_dir)

    episodes = []

    for index, video in enumerate(videos, start=1):

        matched_subtitles = find_subtitles_for_video(
            video,
            subtitles,
        )

        episodes.append(
            EpisodeFile(
                season_number=season_number,
                episode_number=index,
                video_path=video,
                subtitles=matched_subtitles,
            )
        )

    return episodes


# ============================================================================
# File renaming
# ============================================================================

def safe_rename(
    source: Path,
    destination: Path,
) -> Path:

    if source == destination:
        return source

    if destination.exists():
        logger.warning(
            "Rename target already exists: %s",
            destination,
        )
        return destination

    source.rename(destination)

    return destination


def rename_episode(
    episode: EpisodeFile,
):
    """
    Rename:

        episode1.mp4
        episode1.srt

    to:

        S01EP01.mp4
        S01EP01.en.srt
    """

    base_name = (
        f"S{episode.season_number:02d}"
        f"EP{episode.episode_number:02d}"
    )

    # ------------------------------------------------------------
    # Video
    # ------------------------------------------------------------

    old_video = episode.video_path

    new_video = old_video.with_name(
        base_name + old_video.suffix.lower()
    )

    new_video = safe_rename(
        old_video,
        new_video,
    )

    # ------------------------------------------------------------
    # Subtitles
    # ------------------------------------------------------------

    renamed_subtitles = []

    for subtitle in episode.subtitles:

        language_code, _ = guess_subtitle_language(
            subtitle
        )

        new_subtitle_name = (
            f"{base_name}."
            f"{language_code}"
            f"{subtitle.suffix.lower()}"
        )

        new_subtitle = subtitle.with_name(
            new_subtitle_name
        )

        new_subtitle = safe_rename(
            subtitle,
            new_subtitle,
        )

        renamed_subtitles.append(
            (
                new_subtitle,
                language_code,
            )
        )

    return new_video, renamed_subtitles


# ============================================================================
# TMDB
# ============================================================================

class TmdbClient:

    def __init__(self):

        if not TMDB_API_KEY:
            raise RuntimeError(
                "TMDB_API_KEY is missing from .env"
            )

        self.session = requests.Session()

    def get(
        self,
        endpoint: str,
        params: Optional[dict] = None,
    ):

        params = dict(params or {})

        params["api_key"] = TMDB_API_KEY
        params["language"] = "en-US"

        url = (
            f"{TMDB_API_BASE}"
            f"{endpoint}"
        )

        for attempt in range(3):

            response = self.session.get(
                url,
                params=params,
                timeout=15,
            )

            if response.status_code == 429:

                retry_after = int(
                    response.headers.get(
                        "Retry-After",
                        "2",
                    )
                )

                logger.warning(
                    "TMDB rate limit. "
                    "Waiting %s seconds.",
                    retry_after,
                )

                time.sleep(
                    retry_after
                )

                continue

            if response.status_code >= 500:

                time.sleep(
                    attempt + 1
                )

                continue

            if response.status_code == 404:
                return None

            response.raise_for_status()

            return response.json()

        raise RuntimeError(
            f"TMDB request failed: {url}"
        )

    def search_movie(
        self,
        title: str,
        year: Optional[int],
    ):

        params = {
            "query": title,
            "include_adult": "false",
        }

        if year:
            params["year"] = year

        data = self.get(
            "/search/movie",
            params,
        )

        results = (
            data.get("results", [])
            if data
            else []
        )

        # If year search failed, try title only.
        if not results and year:

            data = self.get(
                "/search/movie",
                {
                    "query": title,
                    "include_adult": "false",
                },
            )

            results = (
                data.get("results", [])
                if data
                else []
            )

        return results[0] if results else None

    def get_movie(
        self,
        tmdb_id: int,
    ):

        return self.get(
            f"/movie/{tmdb_id}",
            {
                "append_to_response": "credits",
            },
        )

    def search_show(
        self,
        title: str,
        year: Optional[int],
    ):

        params = {
            "query": title,
        }

        if year:
            params[
                "first_air_date_year"
            ] = year

        data = self.get(
            "/search/tv",
            params,
        )

        results = (
            data.get("results", [])
            if data
            else []
        )

        if not results and year:

            data = self.get(
                "/search/tv",
                {
                    "query": title,
                },
            )

            results = (
                data.get("results", [])
                if data
                else []
            )

        return results[0] if results else None

    def get_show(
        self,
        tmdb_id: int,
    ):

        return self.get(
            f"/tv/{tmdb_id}"
        )

    def get_season(
        self,
        tmdb_id: int,
        season_number: int,
    ):

        return self.get(
            f"/tv/{tmdb_id}/season/{season_number}"
        )


# ============================================================================
# TMDB images
# ============================================================================

def download_image(
    tmdb: TmdbClient,
    relative_path: Optional[str],
    destination: Path,
):

    if not relative_path:
        return None

    destination.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    if destination.exists():
        return destination

    url = (
        f"{TMDB_IMAGE_BASE}"
        f"{relative_path}"
    )

    try:

        response = tmdb.session.get(
            url,
            timeout=30,
        )

        response.raise_for_status()

        with open(
            destination,
            "wb",
        ) as file:

            file.write(
                response.content
            )

        return destination

    except requests.RequestException as error:

        logger.warning(
            "Could not download image %s: %s",
            url,
            error,
        )

        return None


# ============================================================================
# Database
# ============================================================================

def create_database_connection():

    required = {
        "POSTGRES_USER": POSTGRES_USER,
        "POSTGRES_PASSWORD": POSTGRES_PASSWORD,
    }

    missing = [
        name
        for name, value in required.items()
        if not value
    ]

    if missing:

        raise RuntimeError(
            "Missing database configuration in .env: "
            + ", ".join(missing)
        )

    return psycopg2.connect(
        host=POSTGRES_HOST,
        port=POSTGRES_PORT,
        dbname=POSTGRES_DB,
        user=POSTGRES_USER,
        password=POSTGRES_PASSWORD,
    )


# ============================================================================
# Database upserts
# ============================================================================

def upsert_movie(
    cursor,
    details,
    poster_path,
    backdrop_path,
):

    cursor.execute(
        """
        INSERT INTO movies (
            tmdb_id,
            canonical_title,
            release_date,
            overview,
            poster_path,
            backdrop_path,
            runtime_minutes,
            creator_names
        )
        VALUES (
            %s,%s,%s,%s,%s,%s,%s,%s
        )
        ON CONFLICT (tmdb_id)
        DO UPDATE SET
            canonical_title = EXCLUDED.canonical_title,
            release_date = EXCLUDED.release_date,
            overview = EXCLUDED.overview,
            poster_path = EXCLUDED.poster_path,
            backdrop_path = EXCLUDED.backdrop_path,
            runtime_minutes = EXCLUDED.runtime_minutes,
            creator_names = EXCLUDED.creator_names,
            updated_at = now()
        RETURNING id
        """,
        (
            details["id"],
            details.get("title"),
            parse_date(
                details.get("release_date")
            ),
            details.get("overview"),
            poster_path,
            backdrop_path,
            details.get("runtime"),
            get_movie_directors(details),
        ),
    )

    return cursor.fetchone()[0]


def get_movie_directors(details):

    crew = (
        details
        .get("credits", {})
        .get("crew", [])
    )

    directors = [
        person["name"]
        for person in crew
        if person.get("job") == "Director"
    ]

    return ", ".join(directors) or None


def upsert_show(
    cursor,
    details,
    poster_path,
    backdrop_path,
):

    creators = ", ".join(
        person["name"]
        for person in details.get(
            "created_by",
            [],
        )
    ) or None

    cursor.execute(
        """
        INSERT INTO shows (
            tmdb_id,
            canonical_title,
            first_air_date,
            overview,
            poster_path,
            creator_names,
            backdrop_path,
            status
        )
        VALUES (
            %s,%s,%s,%s,%s,%s,%s,%s
        )
        ON CONFLICT (tmdb_id)
        DO UPDATE SET
            canonical_title = EXCLUDED.canonical_title,
            first_air_date = EXCLUDED.first_air_date,
            overview = EXCLUDED.overview,
            poster_path = EXCLUDED.poster_path,
            creator_names = EXCLUDED.creator_names,
            backdrop_path = EXCLUDED.backdrop_path,
            status = EXCLUDED.status,
            updated_at = now()
        RETURNING id
        """,
        (
            details["id"],
            details.get("name"),
            parse_date(
                details.get("first_air_date")
            ),
            details.get("overview"),
            poster_path,
            creators,
            backdrop_path,
            details.get("status"),
        ),
    )

    return cursor.fetchone()[0]


def upsert_season(
    cursor,
    show_id,
    season_details,
    season_number,
    poster_path,
):

    cursor.execute(
        """
        INSERT INTO seasons (
            show_id,
            season_number,
            tmdb_season_id,
            overview,
            poster_path,
            air_date
        )
        VALUES (
            %s,%s,%s,%s,%s,%s
        )
        ON CONFLICT (
            show_id,
            season_number
        )
        DO UPDATE SET
            tmdb_season_id = EXCLUDED.tmdb_season_id,
            overview = EXCLUDED.overview,
            poster_path = EXCLUDED.poster_path,
            air_date = EXCLUDED.air_date
        RETURNING id
        """,
        (
            show_id,
            season_number,
            season_details.get("id")
            if season_details
            else None,
            season_details.get("overview")
            if season_details
            else None,
            poster_path,
            parse_date(
                season_details.get("air_date")
            )
            if season_details
            else None,
        ),
    )

    return cursor.fetchone()[0]


def upsert_episode(
    cursor,
    season_id,
    episode_number,
    tmdb_episode,
    still_path,
):

    cursor.execute(
        """
        INSERT INTO episodes (
            season_id,
            episode_number,
            tmdb_episode_id,
            title,
            overview,
            air_date,
            runtime_minutes,
            still_path
        )
        VALUES (
            %s,%s,%s,%s,%s,%s,%s,%s
        )
        ON CONFLICT (
            season_id,
            episode_number
        )
        DO UPDATE SET
            tmdb_episode_id = EXCLUDED.tmdb_episode_id,
            title = EXCLUDED.title,
            overview = EXCLUDED.overview,
            air_date = EXCLUDED.air_date,
            runtime_minutes = EXCLUDED.runtime_minutes,
            still_path = EXCLUDED.still_path
        RETURNING id
        """,
        (
            season_id,
            episode_number,
            tmdb_episode.get("id")
            if tmdb_episode
            else None,
            (
                tmdb_episode.get("name")
                if tmdb_episode
                else None
            ) or f"Episode {episode_number}",
            tmdb_episode.get("overview")
            if tmdb_episode
            else None,
            parse_date(
                tmdb_episode.get("air_date")
            )
            if tmdb_episode
            else None,
            tmdb_episode.get("runtime")
            if tmdb_episode
            else None,
            still_path,
        ),
    )

    return cursor.fetchone()[0]


def upsert_media_file(
    cursor,
    movie_id,
    episode_id,
    path,
    original_filename,
):

    cursor.execute(
        """
        INSERT INTO media_files (
            movie_id,
            episode_id,
            file_path,
            canonical_filename,
            original_filename,
            file_size_bytes
        )
        VALUES (
            %s,%s,%s,%s,%s,%s
        )
        ON CONFLICT (file_path)
        DO UPDATE SET
            movie_id = EXCLUDED.movie_id,
            episode_id = EXCLUDED.episode_id,
            canonical_filename = EXCLUDED.canonical_filename,
            original_filename =
                COALESCE(
                    media_files.original_filename,
                    EXCLUDED.original_filename
                ),
            file_size_bytes = EXCLUDED.file_size_bytes
        RETURNING id
        """,
        (
            movie_id,
            episode_id,
            str(path),

            # Canonical filename = filename without extension.
            #
            # Example:
            #   movie.mp4   -> movie
            #   S01EP01.mkv -> S01EP01
            #
            path.stem,

            original_filename,
            path.stat().st_size,
        ),
    )

    return cursor.fetchone()[0]


def upsert_subtitle(
    cursor,
    media_file_id,
    path,
    language_code,
    is_default,
):

    label = LANGUAGE_CODES.get(
        language_code,
        language_code.upper(),
    )

    cursor.execute(
        """
        INSERT INTO subtitles (
            media_file_id,
            file_path,
            language_code,
            label,
            format,
            is_default,
            source
        )
        VALUES (
            %s,%s,%s,%s,%s,%s,'external'
        )
        ON CONFLICT (file_path)
        DO UPDATE SET
            media_file_id = EXCLUDED.media_file_id,
            language_code = EXCLUDED.language_code,
            label = EXCLUDED.label,
            format = EXCLUDED.format,
            is_default = EXCLUDED.is_default
        RETURNING id
        """,
        (
            media_file_id,
            str(path),
            language_code,
            label,
            path.suffix.lstrip(".").lower(),
            is_default,
        ),
    )

    return cursor.fetchone()[0]


# ============================================================================
# Movie processing
# ============================================================================

def process_movie(
    movie_dir: Path,
    root: Path,
    assets_dir: Path,
    tmdb: TmdbClient,
    connection,
):

    title, year = parse_title_year(
        movie_dir.name
    )

    logger.info(
        "Processing movie: %s (%s)",
        title,
        year,
    )

    videos = find_video_files(
        movie_dir
    )

    if not videos:

        logger.warning(
            "No video files found in %s",
            movie_dir,
        )

        return

    match = tmdb.search_movie(
        title,
        year,
    )

    if not match:

        logger.warning(
            "TMDB could not find movie: %s (%s)",
            title,
            year,
        )

        return

    details = tmdb.get_movie(
        match["id"]
    )

    if not details:
        return

    tmdb_id = details["id"]

    movie_assets = (
        assets_dir
        / "movies"
        / str(tmdb_id)
    )

    poster = download_image(
        tmdb,
        details.get("poster_path"),
        movie_assets / "poster.jpg",
    )

    backdrop = download_image(
        tmdb,
        details.get("backdrop_path"),
        movie_assets / "backdrop.jpg",
    )

    with connection:

        with connection.cursor() as cursor:

            movie_id = upsert_movie(
                cursor,
                details,
                str(poster)
                if poster
                else None,
                str(backdrop)
                if backdrop
                else None,
            )

            subtitles = find_subtitle_files(
                movie_dir
            )

            for video in videos:

                media_file_id = upsert_media_file(
                    cursor,
                    movie_id,
                    None,
                    video,
                    video.name,
                )

                matched_subtitles = (
                    find_subtitles_for_video(
                        video,
                        subtitles,
                    )
                )

                for index, subtitle in enumerate(
                    matched_subtitles
                ):

                    language, _ = (
                        guess_subtitle_language(
                            subtitle
                        )
                    )

                    upsert_subtitle(
                        cursor,
                        media_file_id,
                        subtitle,
                        language,
                        index == 0,
                    )

    logger.info(
        "Catalogued movie: %s",
        details.get("title"),
    )


# ============================================================================
# Show processing
# ============================================================================

def process_show(
    show_dir: Path,
    root: Path,
    assets_dir: Path,
    tmdb: TmdbClient,
    connection,
):

    title, year = parse_title_year(
        show_dir.name
    )

    logger.info(
        "Processing show: %s (%s)",
        title,
        year,
    )

    match = tmdb.search_show(
        title,
        year,
    )

    if not match:

        logger.warning(
            "TMDB could not find show: %s (%s)",
            title,
            year,
        )

        return

    details = tmdb.get_show(
        match["id"]
    )

    if not details:
        return

    tmdb_id = details["id"]

    show_assets = (
        assets_dir
        / "shows"
        / str(tmdb_id)
    )

    poster = download_image(
        tmdb,
        details.get("poster_path"),
        show_assets / "poster.jpg",
    )

    backdrop = download_image(
        tmdb,
        details.get("backdrop_path"),
        show_assets / "backdrop.jpg",
    )

    with connection:

        with connection.cursor() as cursor:

            show_id = upsert_show(
                cursor,
                details,
                str(poster)
                if poster
                else None,
                str(backdrop)
                if backdrop
                else None,
            )

            for season_number, season_dir in find_season_directories(
                show_dir
            ):

                episodes = build_episodes(
                    season_number,
                    season_dir,
                )

                if not episodes:
                    continue

                season_details = tmdb.get_season(
                    tmdb_id,
                    season_number,
                )

                season_assets = (
                    show_assets
                    / f"season-{season_number}"
                )

                season_poster = None

                if season_details:

                    season_poster = download_image(
                        tmdb,
                        season_details.get(
                            "poster_path"
                        ),
                        season_assets
                        / "poster.jpg",
                    )

                season_id = upsert_season(
                    cursor,
                    show_id,
                    season_details,
                    season_number,
                    str(season_poster)
                    if season_poster
                    else None,
                )

                tmdb_episodes = {}

                if season_details:

                    tmdb_episodes = {
                        episode["episode_number"]: episode
                        for episode in season_details.get(
                            "episodes",
                            [],
                        )
                    }

                for episode in episodes:

                    original_video_name = (
                        episode.video_path.name
                    )

                    new_video, subtitles = (
                        rename_episode(
                            episode
                        )
                    )

                    tmdb_episode = (
                        tmdb_episodes.get(
                            episode.episode_number
                        )
                    )

                    still = None

                    if tmdb_episode:

                        still = download_image(
                            tmdb,
                            tmdb_episode.get(
                                "still_path"
                            ),
                            season_assets
                            / f"episode-{episode.episode_number}"
                            / "still.jpg",
                        )

                    episode_id = upsert_episode(
                        cursor,
                        season_id,
                        episode.episode_number,
                        tmdb_episode,
                        str(still)
                        if still
                        else None,
                    )

                    media_file_id = upsert_media_file(
                        cursor,
                        None,
                        episode_id,
                        new_video,
                        original_video_name,
                    )

                    for index, (
                        subtitle,
                        language,
                    ) in enumerate(
                        subtitles
                    ):

                        upsert_subtitle(
                            cursor,
                            media_file_id,
                            subtitle,
                            language,
                            index == 0,
                        )

    logger.info(
        "Catalogued show: %s",
        details.get("name"),
    )


# ============================================================================
# Main
# ============================================================================

def main():

    logging.basicConfig(
        level=logging.INFO,
        format=(
            "%(asctime)s "
            "%(levelname)s "
            "%(message)s"
        ),
        datefmt="%H:%M:%S",
    )

    root = Path(
        os.environ.get(
            "MEDIA_LIBRARY_ROOT",
            ".",
        )
    ).resolve()

    movies_dir = root / "movies"
    shows_dir = root / "shows"
    assets_dir = root / "assets"

    logger.info(
        "Library root: %s",
        root,
    )

    logger.info(
        "Assets directory: %s",
        assets_dir,
    )

    tmdb = TmdbClient()

    connection = create_database_connection()

    try:

        # ------------------------------------------------------------
        # Movies
        # ------------------------------------------------------------

        if movies_dir.is_dir():

            movie_directories = sorted(
                p
                for p in movies_dir.iterdir()
                if p.is_dir()
            )

            for movie_dir in movie_directories:

                try:

                    process_movie(
                        movie_dir,
                        root,
                        assets_dir,
                        tmdb,
                        connection,
                    )

                except Exception:

                    logger.exception(
                        "Failed to process movie: %s",
                        movie_dir,
                    )

        # ------------------------------------------------------------
        # Shows
        # ------------------------------------------------------------

        if shows_dir.is_dir():

            show_directories = sorted(
                p
                for p in shows_dir.iterdir()
                if p.is_dir()
            )

            for show_dir in show_directories:

                try:

                    process_show(
                        show_dir,
                        root,
                        assets_dir,
                        tmdb,
                        connection,
                    )

                except Exception:

                    logger.exception(
                        "Failed to process show: %s",
                        show_dir,
                    )

    finally:

        connection.close()

    logger.info(
        "Library scan complete."
    )


if __name__ == "__main__":
    main()