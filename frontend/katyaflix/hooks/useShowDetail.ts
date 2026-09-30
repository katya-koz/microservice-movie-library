"use client";

import { useQuery } from "@tanstack/react-query";
import { getShowDetail } from "@/api/shows";

export function useShowDetail(id: string) {
  return useQuery({
    queryKey: ["show", id],
    queryFn: () => getShowDetail(id),
    enabled: !!id,
  });
}
