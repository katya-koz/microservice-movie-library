import { useQuery } from "@tanstack/react-query";
import { getShows } from "@/api/shows";

type UseShowsOptions = {
  page?: number;
  size?: number;
  sort?: string;
};

export function useShows({
  page = 0,
  size = 24,
  sort = "title,asc",
}: UseShowsOptions = {}) {
  return useQuery({
    queryKey: ["shows", page, size, sort],
    queryFn: () => getShows(page, size, sort),
    placeholderData: (previousData) => previousData,
  });
}
