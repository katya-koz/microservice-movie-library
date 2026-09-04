"use client";

import { useSearchParams } from "next/navigation";

import PaginatedMediaPage from "@/components/media/PaginatedMediaPage";
import { useMovies } from "@/hooks/useMovies";
import Poster from "@/components/Poster";

export default function MoviesPage() {
  const searchParams = useSearchParams();

  const page = Number(searchParams.get("page")) || 1;

  // Spring uses zero-based pages.
  const backendPage = page - 1;

  const { data, isLoading, error } = useMovies({
    page: backendPage,
    size: 24,
    sort: "title,asc",
  });

  return (
    <PaginatedMediaPage
      title="Movies"
      media={data?.content ?? []}
      totalElements={data?.totalElements ?? 0}
      totalPages={data?.totalPages ?? 0}
      currentPage={page}
      loading={isLoading}
      error={error}
      renderItem={(movie) => (
        <Poster key={movie.id} type="MOVIE" summary={movie}></Poster>
      )}
    />
  );
}
