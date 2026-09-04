import { TmdbDetails, TmdbSearchResult, TmdbSeasonEpisode } from "./types";

export type TmdbMediaType = "movie" | "tv";

// All TMDB calls are proxied through the Spring Boot upload service, which
// holds the TMDB API key server-side. This runs client-side (the upload
// page's search-as-you-type), so it needs the full absolute URL, not a
// same-origin relative path — see NEXT_PUBLIC_UPLOAD_API_URL in
// .env.local.example.
const UPLOAD_API_URL =
  process.env.NEXT_PUBLIC_UPLOAD_API_URL ?? "http://localhost:8080";

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

// export async function searchTmdb(
//   type: TmdbMediaType,
//   query: string,
// ): Promise<TmdbSearchResult[]> {
//   if (!query.trim()) return [];
//   const res = await fetch(
//     `${UPLOAD_API_URL}/api/tmdb/search?type=${type}&query=${encodeURIComponent(query)}`,
//   );
//   const data = await res.json();
//   if (!res.ok) throw new Error(data.error || "TMDB search failed");
//   return data.results as TmdbSearchResult[];
// }

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
