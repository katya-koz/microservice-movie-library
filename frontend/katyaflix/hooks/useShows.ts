import { useQuery } from "@tanstack/react-query";
import { getShows } from "@/api/shows";
import { PagedQueryOptions } from "@/types/media";

export function useShows({
  page = 0,
  size = 24,
  sort = "title,asc",
  title,
}: PagedQueryOptions = {}) {
  return useQuery({
    queryKey: ["shows", page, size, sort, title],
    queryFn: () => getShows(page, size, sort, title),
    placeholderData: (previousData) => previousData,
  });
}
