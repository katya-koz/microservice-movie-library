"use client";

import { useQuery } from "@tanstack/react-query";
import { getMovieDetail } from "@/api/movies";

export function useMovieDetail(id: string) {
  return useQuery({
    queryKey: ["movie", id],
    queryFn: () => getMovieDetail(id),
    enabled: !!id,
  });
}
