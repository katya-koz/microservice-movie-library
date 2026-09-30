import { API_URL_ROOT } from "@/lib/config";
import { RawSummary } from "@/types/media";
import { MoviePage, MovieDetail } from "@/types/movie";

export async function getMovies(
  page = 0,
  size = 24,
  sort = "title,asc",
  title?: string,
): Promise<MoviePage> {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
    sort,
  });

  if (title) {
    params.set("title", title);
  }

  const response = await fetch(`${API_URL_ROOT}/movies?${params}`);

  if (!response.ok) {
    throw new Error("Failed to fetch movies");
  }

  return response.json();
}

export async function getMovieDetail(id: string): Promise<MovieDetail> {
  const response = await fetch(
    `${API_URL_ROOT}/movies/${encodeURIComponent(id)}`,
  );

  if (!response.ok) {
    throw new Error(`Failed to fetch movie: ${response.status}`);
  }

  return response.json();
}

// Used by useMediaSummaries to hydrate watchlist/currently-watching rows.
// (There used to be a second, near-identical getMovieSummaries() hitting
// this same endpoint and returning the heavier MovieSummary shape — nothing
// actually needed those extra fields, so it's gone; use this everywhere.)
export async function fetchMovieSummaries(
  ids: string[],
): Promise<RawSummary[]> {
  if (ids.length === 0) return [];
  const params = new URLSearchParams();
  ids.forEach((id) => params.append("ids", id));
  const res = await fetch(
    `${API_URL_ROOT}/movies/summary?${params.toString()}`,
  );
  if (!res.ok) throw new Error("Failed to load movie summaries");
  return res.json();
}
