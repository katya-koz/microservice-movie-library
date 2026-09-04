import { MoviePage } from "@/types/movie";

export async function getMovies(
  page = 0,
  size = 24,
  sort = "title,asc",
): Promise<MoviePage> {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
    sort,
  });

  const response = await fetch(
    `${process.env.NEXT_PUBLIC_API_ROOT}/movies?${params}`,
  );

  if (!response.ok) {
    throw new Error("Failed to fetch movies");
  }

  return response.json();
}

import { MovieDetail } from "@/types/movie";

export async function getMovieDetail(id: string): Promise<MovieDetail> {
  const response = await fetch(
    `${process.env.NEXT_PUBLIC_API_ROOT}/movies/${encodeURIComponent(id)}`,
  );

  if (!response.ok) {
    throw new Error(`Failed to fetch movie: ${response.status}`);
  }

  return response.json();
}
