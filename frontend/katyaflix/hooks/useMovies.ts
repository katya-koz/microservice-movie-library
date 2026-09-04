import { useQuery } from "@tanstack/react-query";
import { getMovies } from "@/api/movies";

type UseMoviesOptions = {
  page?: number;
  size?: number;
  sort?: string;
};

export function useMovies({
  page = 0,
  size = 24,
  sort = "title,asc",
}: UseMoviesOptions = {}) {
  return useQuery({
    queryKey: ["movies", page, size, sort],
    queryFn: () => getMovies(page, size, sort),
    placeholderData: (previousData) => previousData,
  });
}
