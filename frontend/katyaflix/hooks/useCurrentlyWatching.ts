"use client";

import { useQuery } from "@tanstack/react-query";
import { USER_SERVICE_URL } from "@/lib/config";
import {
  CurrentlyWatchingMedia,
  CurrentEpisode,
} from "@/types/currentlyWatching";
import {
  fetchCurrentlyWatching,
  fetchCurrentEpisode,
} from "@/api/currentlyWatching";

// The "Continue Watching" row for the home page — movies and shows the
// user has an in-progress watchtime for.
export function useCurrentlyWatching(userId?: string) {
  return useQuery({
    queryKey: ["currently-watching", userId],
    queryFn: () => fetchCurrentlyWatching(userId as string),
    enabled: !!userId,
  });
}

// Where a specific show's in-progress episode is. Used by the Play button
// on the show detail page.
export function useCurrentEpisode(userId?: string, showId?: string) {
  return useQuery({
    queryKey: ["current-episode", userId, showId],
    queryFn: () => fetchCurrentEpisode(userId as string, showId as string),
    enabled: !!userId && !!showId,
  });
}
