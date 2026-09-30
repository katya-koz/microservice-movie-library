"use client";

import { useSearchParams } from "next/navigation";

import PaginatedMediaPage from "@/components/media/PaginatedMediaPage";
import SearchFeature from "@/components/media/SearchFeature";
import { useMovies } from "@/hooks/useMovies";
import Poster from "@/components/Poster";

export default function MoviesPage() {
  const searchParams = useSearchParams();

  const page = Number(searchParams.get("page")) || 1;
  const title = searchParams.get("title") ?? undefined;

  const { data, isLoading, error } = useMovies({
    page: page - 1,
    size: 24,
    sort: "title,asc",
    title,
  });

  return (
    <SearchFeature placeholder="Search movies">
      <PaginatedMediaPage
        title="Movies"
        media={data?.content ?? []}
        totalElements={data?.totalElements ?? 0}
        totalPages={data?.totalPages ?? 0}
        currentPage={page}
        loading={isLoading}
        error={error}
        renderItem={(movie) => (
          <Poster
            key={movie.id}
            summary={{
              id: movie.id,
              mediaType: "MOVIE",
              title: movie.title,
              posterPath: movie.posterPath,
            }}
          />
        )}
      />
    </SearchFeature>
  );
}
