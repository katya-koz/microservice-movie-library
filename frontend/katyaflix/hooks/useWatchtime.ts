"use client";

import { useQuery } from "@tanstack/react-query";
import {
  fetchAllWatchtimes,
  fetchMovieWatchtime,
  fetchEpisodeWatchtime,
  fetchEpisodeWatchtimes,
} from "@/api/watchtime";

export type { WatchtimeMediaType, Watchtime } from "@/types/watchtime";

// All watch times for a profile in one call — use this for profile-wide
// views like a "continue watching" row.
export function useWatchtimes(userId?: string) {
  return useQuery({
    queryKey: ["watchtimes", userId],
    queryFn: () => fetchAllWatchtimes(userId as string),
    enabled: !!userId,
  });
}

// Single movie's watch time — used by the player to resume from where the
// user left off.
export function useMovieWatchtime(userId?: string, movieId?: string) {
  return useQuery({
    queryKey: ["watchtime", "movie", userId, movieId],
    queryFn: () => fetchMovieWatchtime(userId as string, movieId as string),
    enabled: !!userId && !!movieId,
  });
}

export function useEpisodeWatchtime(userId?: string, episodeId?: string) {
  return useQuery({
    queryKey: ["watchtime", "episode", userId, episodeId],
    queryFn: () => fetchEpisodeWatchtime(userId as string, episodeId as string),
    enabled: !!userId && !!episodeId,
  });
}

// Watch times for a specific set of episode ids in one call — use this to
// render progress bars across a single season's episode list without
// pulling down the profile's entire watch history via useWatchtimes.
export function useEpisodeWatchtimes(userId?: string, episodeIds?: string[]) {
  return useQuery({
    queryKey: ["watchtime", "episodes", userId, episodeIds],
    queryFn: () =>
      fetchEpisodeWatchtimes(userId as string, episodeIds as string[]),
    enabled: !!userId && !!episodeIds?.length,
  });
}
