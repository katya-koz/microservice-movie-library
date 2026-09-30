import { TmdbDetails, TmdbSearchResult, TmdbSeasonEpisode } from "@/types/tmdb";
import { UPLOAD_API_URL } from "./config";
export type TmdbMediaType = "movie" | "tv";

export async function searchTmdb(
  type: TmdbMediaType,
  query: string,
  signal?: AbortSignal,
): Promise<TmdbSearchResult[]> {
  const res = await fetch(
    `${UPLOAD_API_URL}/api/tmdb/search?type=${type}&query=${encodeURIComponent(query)}`,
    { signal },
  );

  const data = await res.json();
  if (!res.ok) throw new Error(data.error || "TMDB search failed");
  return data.results as TmdbSearchResult[];
}

export async function getTmdbDetails(
  type: TmdbMediaType,
  id: number,
): Promise<TmdbDetails> {
  const res = await fetch(
    `${UPLOAD_API_URL}/api/tmdb/details?type=${type}&id=${id}`,
  );
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || "TMDB details lookup failed");
  return data as TmdbDetails;
}

/** Used at show-upload time to match uploaded episode files to real titles/overviews. */
export async function getTmdbSeasonEpisodes(
  tvId: number,
  seasonNumber: number,
): Promise<TmdbSeasonEpisode[]> {
  const res = await fetch(
    `${UPLOAD_API_URL}/api/tmdb/season?tvId=${tvId}&season=${seasonNumber}`,
  );
  if (!res.ok) return [];
  const data = await res.json();
  return data.episodes as TmdbSeasonEpisode[];
}

export function tmdbImage(
  path: string | null,
  size: "w200" | "w342" | "w500" | "w1280" | "original" = "w342",
) {
  if (!path) return null;
  return `https://image.tmdb.org/t/p/${size}${path}`;
}
