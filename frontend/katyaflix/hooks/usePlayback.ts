"use client";

import {
  fetchCurrentEpisode,
  fetchFirstEpisode,
} from "@/api/currentlyWatching";
import {
  fetchMoviePlayback,
  fetchEpisodePlayback,
  fetchNextEpisode,
  fetchNextEpisodeRef,
} from "@/api/playback";
import { useMutation, useQuery } from "@tanstack/react-query";

export function useMoviePlayback(movieId: string | undefined) {
  return useQuery({
    queryKey: ["playback", "movie", movieId],
    queryFn: () => fetchMoviePlayback(movieId as string),
    enabled: !!movieId,
    staleTime: 60_000,
    retry: 1,
  });
}

export function useEpisodePlayback(episodeId: string | undefined) {
  return useQuery({
    queryKey: ["playback", "episode", episodeId],
    queryFn: () => fetchEpisodePlayback(episodeId as string),
    enabled: !!episodeId,
    staleTime: 60_000,
    retry: 1,
  });
}

export function useNextEpisode(
  episodeId: string | undefined,
  options?: { enabled?: boolean },
) {
  return useQuery({
    queryKey: ["playback", "episode", episodeId, "next"],
    queryFn: () => fetchNextEpisode(episodeId as string),
    enabled: !!episodeId && (options?.enabled ?? true),
    staleTime: 60_000,
    retry: 1,
  });
}

function buildEpisodeWatchUrl(
  showId: string,
  seasonId: string,
  episodeId: string,
) {
  const params = new URLSearchParams({
    show_id: showId,
    season_id: seasonId,
    episode_id: episodeId,
  });
  return `/watch/episode?${params.toString()}`;
}

const RESUME_THRESHOLD = 0.95;

async function resolveShowPlaybackTarget(
  userId: string,
  showId: string,
): Promise<string | null> {
  let current = await fetchCurrentEpisode(userId, showId);

  if (!current) {
    current = await fetchFirstEpisode(showId);
  }

  if (!current) return null; // this shouldnt really ever happen

  const progress =
    current.durationSeconds > 0
      ? current.watchedSeconds / current.durationSeconds
      : 0;

  if (progress >= RESUME_THRESHOLD) {
    const next = await fetchNextEpisodeRef(current.episodeId);
    if (next) {
      return buildEpisodeWatchUrl(next.showId, next.seasonId, next.episodeId);
    }
  }

  return buildEpisodeWatchUrl(
    current.showId,
    current.seasonId,
    current.episodeId,
  );
}

// Resolves where the Play button on a show's detail page should navigate.
// The watch page itself picks up the saved seek position via
// useEpisodeWatchtime, so this only needs to decide *which* episode.
export function useResolveShowPlayback(userId?: string) {
  return useMutation({
    mutationFn: (showId: string) =>
      resolveShowPlaybackTarget(userId as string, showId),
  });
}
