"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { API_URL_ROOT } from "@/lib/config";
import { fetchWatchlist } from "@/api/watchlist";
import { MediaType } from "@/types/media";

// Everything on a profile's list — for the "My List" page. Combine this
// with useMediaSummaries to render title cards.
export function useWatchlist(userId?: string) {
  return useQuery({
    queryKey: ["watchlist", userId],
    queryFn: () => fetchWatchlist(userId as string),
    enabled: !!userId,
  });
}

// Convenience hook for a single detail page's "My List" toggle button.
export function useIsOnWatchlist(
  userId: string | undefined,
  mediaType: MediaType | null,
  mediaId: string | null,
) {
  const { data } = useWatchlist(userId);
  return !!data?.some(
    (item) => item.mediaType === mediaType && item.mediaId === mediaId,
  );
}

export function useAddToWatchlist(userId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      mediaType,
      mediaId,
    }: {
      mediaType: MediaType;
      mediaId: string;
    }) => {
      if (!userId) throw new Error("No active profile");
      const res = await fetch(`${API_URL_ROOT}/profiles/${userId}/watchlist`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ mediaType, mediaId }),
      });
      if (!res.ok) throw new Error("Failed to add to list");
      return res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["watchlist", userId] });
    },
  });
}

export function useRemoveFromWatchlist(userId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      mediaType,
      mediaId,
    }: {
      mediaType: MediaType;
      mediaId: string;
    }) => {
      if (!userId) throw new Error("No active profile");
      const params = new URLSearchParams({ mediaType, mediaId });
      const res = await fetch(
        `${API_URL_ROOT}/profiles/${userId}/watchlist?${params.toString()}`,
        { method: "DELETE" },
      );
      if (!res.ok) throw new Error("Failed to remove from list");
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["watchlist", userId] });
    },
  });
}
