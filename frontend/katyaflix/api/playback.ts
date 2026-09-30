import { API_URL_ROOT } from "@/lib/config";
import { PlaybackInfo, NextEpisode, NextEpisodeRef } from "@/types/playback";

export async function fetchMoviePlayback(
  movieId: string,
): Promise<PlaybackInfo> {
  const res = await fetch(`${API_URL_ROOT}/playback/movies/${movieId}`);

  if (!res.ok) {
    throw new Error(`Failed to load playback info (${res.status})`);
  }

  return res.json();
}

export async function fetchEpisodePlayback(
  episodeId: string,
): Promise<PlaybackInfo> {
  const res = await fetch(`${API_URL_ROOT}/playback/episodes/${episodeId}`);

  if (!res.ok) {
    throw new Error(`Failed to load playback info (${res.status})`);
  }

  return res.json();
}

// Full "up next" metadata (title, still, episode/season numbers) for the
// player's preview card. Backs useNextEpisode.
export async function fetchNextEpisode(
  episodeId: string,
): Promise<NextEpisode | null> {
  const res = await fetch(
    `${API_URL_ROOT}/playback/episodes/${episodeId}/next`,
  );

  // 404 means this was the last episode of the show — not an error state.
  if (res.status === 404) {
    return null;
  }

  if (!res.ok) {
    throw new Error(`Failed to load next episode (${res.status})`);
  }

  return res.json();
}

// Just the ids needed to navigate to the next episode. Backs
// resolveShowPlaybackTarget (the show page's Play button) — a different
// endpoint than fetchNextEpisode above, and deliberately not merged with
// it: this one is a navigation lookup, not display metadata. Renamed from
// the old "fetchNextEpisode2" now that it's clear what it's for.
export async function fetchNextEpisodeRef(
  episodeId: string,
): Promise<NextEpisodeRef | null> {
  const res = await fetch(
    `${API_URL_ROOT}/playback/episodes/${episodeId}/next`,
  );
  if (res.status === 404) return null; // was the last episode of the show
  if (!res.ok) throw new Error("Failed to load next episode");
  return res.json();
}
