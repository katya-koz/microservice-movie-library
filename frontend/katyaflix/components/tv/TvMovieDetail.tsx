"use client";

import { useMovieDetail } from "@/hooks/useMovieDetail";
import TvDetail from "./TvDetail";
import { TvMessage, TvSpinner } from "./TvStatus";

export default function TvMovieDetail({ movieId }: { movieId: string }) {
  const { data: movie, isLoading, error } = useMovieDetail(movieId);

  if (isLoading) return <TvSpinner />;
  if (error || !movie) return <TvMessage>Failed to load this movie.</TvMessage>;

  const year = movie.releaseDate ? new Date(movie.releaseDate).getFullYear() : null;

  const subtitle = (
    <>
      {year && <span>{year}</span>}
      {movie.runtimeMinutes && <span>• {movie.runtimeMinutes} min</span>}
      {movie.creatorNames && (
        <span>
          • Directed by <span className="text-white">{movie.creatorNames}</span>
        </span>
      )}
    </>
  );

  return (
    <TvDetail
      title={movie.title}
      backdropPath={movie.backdropPath}
      subtitle={subtitle}
      overview={movie.overview}
      genres={movie.genres ?? []}
      type="MOVIE"
      id={movie.id}
    />
  );
}
