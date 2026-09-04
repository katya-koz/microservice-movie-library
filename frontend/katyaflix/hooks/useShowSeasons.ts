"use client";

import { useShowDetail } from "@/hooks/useShowDetail";
import type { SeasonSummary } from "@/types/show";

type UseShowSeasonsResult = {
  data: SeasonSummary[] | undefined;
  isLoading: boolean;
  error: Error | null;
};

// There's no standalone "seasons" endpoint — season summaries come bundled
// in GET /api/shows/{id}. This hook re-fetches show detail and pulls
// `seasons` off it, so it's a self-contained drop-in for SeasonEpisodesSection.
//
// If the page that renders <DetailPage> already calls useShowDetail (or
// similar) to get title/backdrop/overview, prefer passing `show.seasons`
// down as a prop instead of using this hook — that avoids fetching the same
// show twice.
export function useShowSeasons(showId: string): UseShowSeasonsResult {
  const { data, isLoading, error } = useShowDetail(showId);
  return { data: data?.seasons, isLoading, error };
}
