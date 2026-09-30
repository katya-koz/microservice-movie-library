export type ShowSummary = {
  id: string;
  tmdbId: number | null;
  title: string;
  releaseDate: string | null;
  posterPath: string | null;
};

export type ShowDetail = {
  id: string;
  tmdbId: number | null;
  title: string;
  firstAirDate: string | null;
  status: string | null;
  overview: string | null;
  posterPath: string | null;
  backdropPath: string | null;
  creatorNames: string[];
  createdAt: string;
  updatedAt: string;
  genres: string[];
  seasons: SeasonSummary[]; // season picker only — not full episode lists
};

export type ShowPage = {
  content: ShowSummary[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
};

export type SeasonSummary = {
  seasonId: string;
  seasonNumber: number;
  title: string;
  posterPath: string | null;
  episodeCount: number;
};

export type EpisodeDetail = {
  id: string;
  episodeNumber: number;
  seasonNumber: number;
  title: string;
  overview: string;
  runtimeMinutes: number | null;
  stillPath: string | null;
  airDate: string | null; // ISO date
};
