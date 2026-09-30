"use client";

import { useQuery } from "@tanstack/react-query";

import { fetchUploadJobs } from "@/api/upload";

export function useUploadJobs(page = 0, size = 20, userId?: string) {
  return useQuery({
    queryKey: ["upload-jobs", page, size, userId],
    queryFn: () => fetchUploadJobs(page, size, userId as string),
    enabled: !!userId,
    // light polling as a fallback for the list view; the selected job's
    // detail panel gets true live updates over the websocket instead
    refetchInterval: 15000,
  });
}
