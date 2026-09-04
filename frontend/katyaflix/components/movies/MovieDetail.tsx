"use client";
import DetailPage from "../media/DetailPage";
import { useMovieDetail } from "@/hooks/useMovieDetail";

type MovieDetailProps = {
  movieId: string;
  modal?: boolean;
};

const MEDIA_URL_ROOT =
  process.env.NEXT_PUBLIC_MEDIA_URL_ROOT ?? "http://localhost:8081/media";

export default function MovieDetail({ movieId }: MovieDetailProps) {
  const { data: movie, isLoading, error } = useMovieDetail(movieId);

  const releaseYear = movie?.releaseDate
    ? new Date(movie?.releaseDate).getFullYear()
    : null;

  const subtitle = (
    <div className="mt-3 flex flex-wrap items-center gap-2 text-sm text-slate-400">
      {releaseYear && <span>{releaseYear}</span>}

      {releaseYear && movie?.runtimeMinutes && <span>•</span>}

      {movie?.runtimeMinutes && <span>{movie?.runtimeMinutes} min</span>}

      {movie?.creatorNames && <span>•</span>}

      {movie?.creatorNames && (
        <span>
          Directed by{" "}
          <span className="text-slate-200">{movie?.creatorNames}</span>
        </span>
      )}
    </div>
  );

  return (
    movie && (
      <DetailPage
        title={movie.title}
        backdropPath={movie.backdropPath}
        subtitle={subtitle}
        isLoading={isLoading}
        error={error}
        overview={movie.overview}
        id={movie.id}
        type="MOVIE"
      ></DetailPage>
    )
  );
}
