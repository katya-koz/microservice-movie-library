import { useQuery } from "@tanstack/react-query";
import { getMovies } from "@/api/movies";
import { PagedQueryOptions } from "@/types/media";

export function useMovies({
  page = 0,
  size = 24,
  sort = "title,asc",
  title,
}: PagedQueryOptions = {}) {
  return useQuery({
    queryKey: ["movies", page, size, sort, title],
    queryFn: () => getMovies(page, size, sort, title),
    placeholderData: (previousData) => previousData,
  });
}
