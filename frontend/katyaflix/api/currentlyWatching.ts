import { API_URL_ROOT } from "@/lib/config";
import {
  CurrentlyWatchingMedia,
  CurrentEpisode,
} from "@/types/currentlyWatching";

export async function fetchCurrentlyWatching(
  userId: string,
): Promise<CurrentlyWatchingMedia[]> {
  const res = await fetch(
    `${API_URL_ROOT}/profiles/${userId}/currently-watching`,
  );
  if (!res.ok) throw new Error("Failed to load currently watching");
  return res.json();
}

// Exported (not just used internally) so usePlayback can reuse it without
// duplicating the fetch.
export async function fetchCurrentEpisode(
  userId: string,
  showId: string,
): Promise<CurrentEpisode | null> {
  const res = await fetch(
    `${API_URL_ROOT}/profiles/${userId}/currently-watching/${showId}`,
  );
  if (res.status === 404) return null; // nothing started for this show yet
  if (!res.ok) throw new Error("Failed to load current episode");
  return res.json();
}
