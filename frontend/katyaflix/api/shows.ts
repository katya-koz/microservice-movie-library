import { API_URL_ROOT } from "@/lib/config";
import { RawSummary } from "@/types/media";
import { ShowDetail, ShowPage, EpisodeDetail } from "@/types/show";

export async function getShows(
  page = 0,
  size = 24,
  sort = "title,asc",
  title?: string,
): Promise<ShowPage> {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
    sort,
  });

  if (title) {
    params.set("title", title);
  }

  const response = await fetch(`${API_URL_ROOT}/shows?${params}`);

  if (!response.ok) {
    throw new Error("Failed to fetch shows");
  }

  return response.json();
}

export async function getShowDetail(id: string): Promise<ShowDetail> {
  const response = await fetch(
    `${API_URL_ROOT}/shows/${encodeURIComponent(id)}`,
  );

  if (!response.ok) {
    throw new Error(`Failed to fetch show: ${response.status}`);
  }

  return response.json();
}

// GET /api/shows/{showId}/seasons/{seasonNumber}/episodes
// Moved here from a hand-rolled useEffect+fetch inside useShowEpisodes.ts so
// that hook can be a plain useQuery wrapper like the rest of the app.
export async function getSeasonEpisodes(
  showId: string,
  seasonNumber: number,
): Promise<EpisodeDetail[]> {
  const response = await fetch(
    `${API_URL_ROOT}/shows/${showId}/seasons/${seasonNumber}/episodes`,
  );

  if (!response.ok) {
    throw new Error(`Failed to fetch episodes: ${response.status}`);
  }

  return response.json();
}

// Used by useMediaSummaries to hydrate watchlist/currently-watching rows.
// (There used to be a second, near-identical getShowSummaries() hitting
// this same endpoint and returning the heavier ShowSummary shape — nothing
// actually needed those extra fields, so it's gone; use this everywhere.)
export async function fetchShowSummaries(ids: string[]): Promise<RawSummary[]> {
  if (ids.length === 0) return [];
  const params = new URLSearchParams();
  ids.forEach((id) => params.append("ids", id));
  const res = await fetch(`${API_URL_ROOT}/shows/summary?${params.toString()}`);
  if (!res.ok) throw new Error("Failed to load show summaries");
  return res.json();
}
