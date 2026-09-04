import { useQuery } from "@tanstack/react-query";
import { getTmdbDetails } from "@/lib/tmdb";

export function useTmdbDetails(type: "movie" | "tv", id: number | null) {
  return useQuery({
    queryKey: ["tmdb-details", type, id],
    queryFn: () => getTmdbDetails(type, id!),
    enabled: id !== null,
  });
}
