"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { MediaRef, MediaSummary, RawSummary } from "@/types/media";
import { fetchMovieSummaries } from "@/api/movies";
import { fetchShowSummaries } from "@/api/shows";

// Splits a mixed list of {mediaType, mediaId} refs (e.g. from the
// watchlist or currently-watching endpoints) into the two catalog summary
// calls, then re-merges the results in the original input order so
// callers can render a stable carousel.
export function useMediaSummaries(items: MediaRef[]) {
  const movieIds = useMemo(
    () => items.filter((i) => i.mediaType === "MOVIE").map((i) => i.mediaId),
    [items],
  );
  const showIds = useMemo(
    () => items.filter((i) => i.mediaType === "SHOW").map((i) => i.mediaId),
    [items],
  );

  const moviesQuery = useQuery({
    queryKey: ["movie-summaries", movieIds],
    queryFn: () => fetchMovieSummaries(movieIds),
    enabled: movieIds.length > 0,
  });

  const showsQuery = useQuery({
    queryKey: ["show-summaries", showIds],
    queryFn: () => fetchShowSummaries(showIds),
    enabled: showIds.length > 0,
  });

  const data = useMemo(() => {
    const movieById = new Map((moviesQuery.data ?? []).map((m) => [m.id, m]));
    const showById = new Map((showsQuery.data ?? []).map((s) => [s.id, s]));

    return items
      .map((item): MediaSummary | null => {
        const raw =
          item.mediaType === "MOVIE"
            ? movieById.get(item.mediaId)
            : showById.get(item.mediaId);
        if (!raw) return null;
        return { ...raw, mediaType: item.mediaType };
      })
      .filter((m): m is MediaSummary => m !== null);
  }, [items, moviesQuery.data, showsQuery.data]);

  return {
    data,
    isLoading:
      (movieIds.length > 0 && moviesQuery.isLoading) ||
      (showIds.length > 0 && showsQuery.isLoading),
    error: moviesQuery.error ?? showsQuery.error,
  };
}
