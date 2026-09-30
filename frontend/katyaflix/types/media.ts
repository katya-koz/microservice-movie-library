// The MOVIE/SHOW union used to be redeclared separately in currentlyWatching.ts,
// myList.ts, and hooks/useWatchlist.ts. This is now the one canonical copy —
// everything else imports it instead of redefining it.
export type MediaType = "MOVIE" | "SHOW";

export type MediaRef = { mediaType: MediaType; mediaId: string };

export type MediaSummary = {
  id: string;
  mediaType: MediaType;
  title: string;
  posterPath: string | null;
};

// Field names (id/title/posterPath) are inferred from how posters are
// used elsewhere (season.posterPath, episode.stillPath) — adjust to match
// the real MovieSummary/ShowSummary records if they differ.
export type RawSummary = {
  id: string;
  title: string;
  posterPath: string | null;
};

// Shared shape for the movies/shows paginated list hooks (useMovies, useShows).
// Was declared twice — once imported from types/movie.ts, once redeclared
// inline in hooks/useShows.ts — with identical fields both times.
export type PagedQueryOptions = {
  page?: number;
  size?: number;
  sort?: string;
  title?: string;
};
