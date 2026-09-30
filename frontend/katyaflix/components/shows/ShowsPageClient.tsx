"use client";

import { useSearchParams } from "next/navigation";
import Poster from "@/components/Poster";
import PaginatedMediaPage from "@/components/media/PaginatedMediaPage";
import SearchFeature from "@/components/media/SearchFeature";
import { useShows } from "@/hooks/useShows";

export default function ShowsPage() {
  const searchParams = useSearchParams();

  const page = Number(searchParams.get("page")) || 1;
  const title = searchParams.get("title") ?? undefined;

  const { data, isLoading, error } = useShows({
    page: page - 1,
    size: 24,
    sort: "title,asc",
    title,
  });

  return (
    <SearchFeature placeholder="Search shows">
      <PaginatedMediaPage
        title="Shows"
        media={data?.content ?? []}
        totalElements={data?.totalElements ?? 0}
        totalPages={data?.totalPages ?? 0}
        currentPage={page}
        loading={isLoading}
        error={error}
        renderItem={(show) => (
          <Poster
            key={show.id}
            summary={{
              id: show.id,
              mediaType: "SHOW",
              title: show.title,
              posterPath: show.posterPath,
            }}
          />
        )}
      />
    </SearchFeature>
  );
}
