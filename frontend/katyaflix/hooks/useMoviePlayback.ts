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

const PLAYBACK_URL_ROOT =
  process.env.NEXT_PUBLIC_PLAYBACK_URL_ROOT ?? "http://localhost:8080/playback";

async function fetchMoviePlayback(movieId: string): Promise<PlaybackInfo> {
  const res = await fetch(`${PLAYBACK_URL_ROOT}/movies/${movieId}`);

  if (!res.ok) {
    throw new Error(`Failed to load playback info (${res.status})`);
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
