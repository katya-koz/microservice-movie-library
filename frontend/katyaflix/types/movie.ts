export type MovieSummary = {
  id: string;
  title: string;
  posterPath: string | null;
};

export type MovieDetail = {
  id: string;
  title: string;
  releaseDate: string | null;
  overview: string | null;
  posterPath: string | null;
  backdropPath: string | null;
  runtimeMinutes: number | null;
  creatorNames: string | null; // comma separated
  genres: string[];
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
