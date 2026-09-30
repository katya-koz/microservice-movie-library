"use client";

import { useShowDetail } from "@/hooks/useShowDetail";
import type { SeasonSummary } from "@/types/show";

type UseShowSeasonsResult = {
  data: SeasonSummary[] | undefined;
  isLoading: boolean;
  error: Error | null;
};

export function useShowSeasons(showId: string): UseShowSeasonsResult {
  const { data, isLoading, error } = useShowDetail(showId);
  return { data: data?.seasons, isLoading, error };
}
