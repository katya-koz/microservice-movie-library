"use client";

import { useQuery } from "@tanstack/react-query";
import { getSeasonEpisodes } from "@/api/shows";

export function useSeasonEpisodes(showId: string, seasonNumber: number | null) {
  return useQuery({
    queryKey: ["show", showId, "season", seasonNumber, "episodes"],
    queryFn: () => getSeasonEpisodes(showId, seasonNumber as number),
    enabled: !!showId && seasonNumber !== null,
  });
}
