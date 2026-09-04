"use client";

import { useQuery } from "@tanstack/react-query";

export type SubtitleTrack = {
  id: string;
  languageCode: string;
  label: string;
  filePath: string;
  isForced: boolean;
  isSdh: boolean;
  isDefault: boolean;
};

export type PlaybackInfo = {
  mediaFileId: string;
  filePath: string;
  containerFormat: string;
  durationSeconds: number;
  resolution: string;
  subtitles: SubtitleTrack[];
};

// NOTE: the backend snippet didn't include the `NextEpisode` record fields —
// this shape is a guess based on the naming used elsewhere (EpisodeDetail).
// Adjust to match once you confirm the real DTO.
export type NextEpisode = {
  episodeId: string;
  episodeNumber: number;
  seasonNumber: number;
  title: string;
  stillPath: string | null;
};

const PLAYBACK_URL_ROOT =
  process.env.NEXT_PUBLIC_PLAYBACK_URL_ROOT ?? "http://localhost:8080/playback";

async function fetchMoviePlayback(movieId: string): Promise<PlaybackInfo> {
  const res = await fetch(`${PLAYBACK_URL_ROOT}/movies/${movieId}`);

  if (!res.ok) {
    throw new Error(`Failed to load playback info (${res.status})`);
  }

  return res.json();
}

async function fetchEpisodePlayback(episodeId: string): Promise<PlaybackInfo> {
  const res = await fetch(`${PLAYBACK_URL_ROOT}/episodes/${episodeId}`);

  if (!res.ok) {
    throw new Error(`Failed to load playback info (${res.status})`);
  }

  return res.json();
}

async function fetchNextEpisode(
  episodeId: string,
): Promise<NextEpisode | null> {
  const res = await fetch(`${PLAYBACK_URL_ROOT}/episodes/${episodeId}/next`);

  // 404 means this was the last episode of the show — not an error state.
  if (res.status === 404) {
    return null;
  }

  if (!res.ok) {
    throw new Error(`Failed to load next episode (${res.status})`);
  }

  return res.json();
}

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
