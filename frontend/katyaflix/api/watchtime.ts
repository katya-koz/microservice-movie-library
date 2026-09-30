import { API_URL_ROOT } from "@/lib/config";
import { Watchtime } from "@/types/watchtime";

export async function fetchAllWatchtimes(userId: string): Promise<Watchtime[]> {
  const res = await fetch(`${API_URL_ROOT}/profiles/${userId}/watchtimes`);
  if (!res.ok) throw new Error("Failed to load watch times");
  return res.json();
}

export async function fetchMovieWatchtime(
  userId: string,
  movieId: string,
): Promise<Watchtime> {
  const res = await fetch(
    `${API_URL_ROOT}/profiles/${userId}/watchtimes/movie/${movieId}`,
  );
  if (!res.ok) throw new Error("Failed to load watch time");
  return res.json();
}

export async function fetchEpisodeWatchtime(
  userId: string,
  episodeId: string,
): Promise<Watchtime> {
  const res = await fetch(
    `${API_URL_ROOT}/profiles/${userId}/watchtimes/episode/${episodeId}`,
  );
  if (!res.ok) throw new Error("Failed to load watch time");
  return res.json();
}

export async function fetchEpisodeWatchtimes(
  userId: string,
  episodeIds: string[],
): Promise<Watchtime[]> {
  const params = new URLSearchParams();
  episodeIds.forEach((id) => params.append("episodeIds", id));
  const res = await fetch(
    `${API_URL_ROOT}/profiles/${userId}/watchtimes/episodes?${params.toString()}`,
  );
  if (!res.ok) throw new Error("Failed to load watch times");
  return res.json();
}
