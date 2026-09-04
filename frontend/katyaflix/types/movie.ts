export type MovieSummary = {
  id: string;
  tmdbId: number | null;
  title: string;
  releaseDate: string | null;
  posterPath: string | null;
};

export type MovieDetail = {
  id: string;
  tmdbId: number | null;
  title: string;
  releaseDate: string | null;
  overview: string | null;
  posterPath: string | null;
  backdropPath: string | null;
  runtimeMinutes: number | null;
  creatorNames: string[];
  createdAt: string;
  updatedAt: string;
};

export type MoviePage = {
  content: MovieSummary[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
};
