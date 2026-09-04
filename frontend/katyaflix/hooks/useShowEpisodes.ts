"use client";

import { useEffect, useState } from "react";
import type { EpisodeDetail } from "@/types/show";

const API_URL_ROOT =
  process.env.NEXT_PUBLIC_API_URL_ROOT ?? "http://localhost:8080/api";

type UseSeasonEpisodesResult = {
  data: EpisodeDetail[] | undefined;
  isLoading: boolean;
  error: Error | null;
};

// GET /api/shows/{showId}/seasons/{seasonNumber}/episodes
export function useSeasonEpisodes(
  showId: string,
  seasonNumber: number | null,
): UseSeasonEpisodesResult {
  const [data, setData] = useState<EpisodeDetail[] | undefined>(undefined);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    if (!showId || seasonNumber === null) {
      setData(undefined);
      setIsLoading(false);
      setError(null);
      return;
    }

    const controller = new AbortController();

    setIsLoading(true);
    setError(null);

    fetch(`${API_URL_ROOT}/shows/${showId}/seasons/${seasonNumber}/episodes`, {
      signal: controller.signal,
    })
      .then(async (res) => {
        if (!res.ok) {
          throw new Error(`Failed to load episodes (${res.status})`);
        }
        return (await res.json()) as EpisodeDetail[];
      })
      .then((episodes) => setData(episodes))
      .catch((err: unknown) => {
        if (err instanceof Error && err.name !== "AbortError") {
          setError(err);
        }
      })
      .finally(() => setIsLoading(false));

    return () => controller.abort();
  }, [showId, seasonNumber]);

  return { data, isLoading, error };
}
