"use client";

import { useSearchParams } from "next/navigation";
import Poster from "@/components/Poster";
import PaginatedMediaPage from "@/components/media/PaginatedMediaPage";
import { useShows } from "@/hooks/useShows";

export default function ShowsPage() {
  const searchParams = useSearchParams();

  const page = Number(searchParams.get("page")) || 1;

  const backendPage = page - 1;

  const { data, isLoading, error } = useShows({
    page: backendPage,
    size: 24,
    sort: "title,asc",
  });

  return (
    <PaginatedMediaPage
      title="Shows"
      media={data?.content ?? []}
      totalElements={data?.totalElements ?? 0}
      totalPages={data?.totalPages ?? 0}
      currentPage={page}
      loading={isLoading}
      error={error}
      renderItem={(show) => (
        <Poster key={show.id} type="SHOW" summary={show}></Poster>
      )}
    />
  );
}
