"use client";

import { useEffect, useRef } from "react";
import type { WatchtimeMediaType } from "@/hooks/useWatchtime";
import { API_URL_ROOT } from "@/lib/config";

const SYNC_INTERVAL_MS = 15_000; // push a position update roughly every 15s

type UseWatchtimeSyncArgs = {
  userId?: string;
  mediaType: WatchtimeMediaType;
  mediaId?: string;
  // Only meaningful (and should only be passed) when mediaType is
  // "EPISODE" — the show/season this episode belongs to.
  showId?: string;
  seasonId?: string;
  // Called to read the current playback position, e.g.
  // () => videoRef.current?.currentTime ?? 0
  getCurrentTimeSeconds: () => number;
  // Called to read the total video duration, e.g.
  // () => videoRef.current?.duration ?? 0
  getDurationSeconds: () => number;
  // Only sync while this is true (i.e. actually playing).
  isActive: boolean;
};
export function useWatchtimeSync({
  userId,
  mediaType,
  mediaId,
  showId,
  seasonId,
  getCurrentTimeSeconds,
  getDurationSeconds,
  isActive,
}: UseWatchtimeSyncArgs) {
  const getCurrentTimeRef = useRef(getCurrentTimeSeconds);
  getCurrentTimeRef.current = getCurrentTimeSeconds;

  const getDurationRef = useRef(getDurationSeconds);
  getDurationRef.current = getDurationSeconds;

  useEffect(() => {
    if (!isActive || !userId || !mediaId) return;

    const sendUpdate = () => {
      const durationSeconds = Math.floor(getDurationRef.current());
      if (!Number.isFinite(durationSeconds) || durationSeconds <= 0) return;
      const watchtimeSeconds = Math.floor(getCurrentTimeRef.current());

      fetch(`${API_URL_ROOT}/profiles/${userId}/watchtimes`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          mediaType,
          mediaId,
          watchtimeSeconds,
          durationSeconds,
          // Only set for episodes; JSON.stringify drops undefined keys
          // entirely, so a movie PUT simply won't include these fields.
          showId,
          seasonId,
        }),
        keepalive: true,
      }).catch((err) => {
        console.error("Failed to sync watchtime", err);
      });
    };

    const interval = setInterval(sendUpdate, SYNC_INTERVAL_MS);

    return () => {
      clearInterval(interval);
      sendUpdate(); // final flush
    };
  }, [isActive, userId, mediaId, mediaType, showId, seasonId]);
}
