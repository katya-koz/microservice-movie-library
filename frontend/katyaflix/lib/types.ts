export interface TmdbSearchResult {
  id: number;
  title: string;
  year: string;
  /** Full ISO date (release_date / first_air_date) if TMDB has one — needed
   * server-side to populate the catalog's DATE columns; `year` alone isn't enough. */
  releaseDate: string | null;
  posterPath: string | null;
  overview: string;
}

export interface TmdbDetails {
  genres: string[];
  runtime: number | null;
  backdropPath: string | null;
  overview: string;
  creators: string[];
  status: string | null; // TV only — "Ended", "Returning Series", etc.
}

export interface TmdbSeasonEpisode {
  episodeNumber: number;
  name: string;
  overview: string;
  stillPath: string | null;
}

export interface SubtitleTrack {
  id: string;
  label: string;
  fileName: string;
  /** Native <track> only understands WebVTT — .srt is converted at upload time. */
  supported: boolean;
}

export interface Movie {
  id: string;
  tmdbId: number;
  title: string;
  year: string;
  overview: string;
  posterPath: string | null;
  backdropPath: string | null;
  genres: string[];
  runtime: number | null;
  videoFileName: string;
  duration?: number;
  subtitles: SubtitleTrack[];
  createdAt: number;
}

export interface Episode {
  id: string;
  number: number;
  title: string;
  fileName: string;
  duration?: number;
  subtitles: SubtitleTrack[];
  overview?: string;
  stillPath?: string | null;
}

export interface Season {
  id: string;
  name: string;
  number: number;
  episodes: Episode[];
}

export interface Show {
  id: string;
  tmdbId: number;
  title: string;
  year: string;
  overview: string;
  posterPath: string | null;
  backdropPath: string | null;
  genres: string[];
  seasons: Season[];
  createdAt: number;
}
