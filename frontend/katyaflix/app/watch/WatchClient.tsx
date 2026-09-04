"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import {
  useMoviePlayback,
  useEpisodePlayback,
  useNextEpisode,
} from "@/hooks/usePlayback";

const MEDIA_URL_ROOT =
  process.env.NEXT_PUBLIC_MEDIA_URL_ROOT ?? "http://localhost:8081/media";

export default function WatchClient() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const movieId = searchParams.get("movie_id") ?? undefined;
  const episodeId = searchParams.get("episode_id") ?? undefined;
  const mediaType = movieId ? "MOVIE" : episodeId ? "EPISODE" : null;

  const [hasEnded, setHasEnded] = useState(false);

  // Reset "ended" state when the id in the URL changes (e.g. after
  // navigating to the next episode).
  useEffect(() => {
    setHasEnded(false);
  }, [movieId, episodeId]);

  // Both hooks are called unconditionally (rules of hooks), but each is
  // internally `enabled` only when its id is present, so only one ever
  // actually fetches.
  const movieQuery = useMoviePlayback(movieId);
  const episodeQuery = useEpisodePlayback(episodeId);
  const {
    data: playback,
    isLoading,
    error,
  } = mediaType === "MOVIE" ? movieQuery : episodeQuery;

  const { data: nextEpisode } = useNextEpisode(episodeId, {
    enabled: mediaType === "EPISODE" && hasEnded,
  });

  const videoSrc = useMemo(
    () => (playback ? `${MEDIA_URL_ROOT}${playback.filePath}` : null),
    [playback],
  );

  const handlePlayNext = () => {
    if (nextEpisode) {
      router.replace(`/watch/episode?episode_id=${nextEpisode.episodeId}`);
    }
  };

  if (!mediaType) {
    return (
      <div className="fixed inset-0 flex items-center justify-center bg-black">
        <p className="text-red-400">No media specified.</p>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 z-[200] flex items-center justify-center bg-black">
      <button
        onClick={() => router.back()}
        className="absolute right-6 top-6 z-10 rounded-full bg-slate-800/80 px-3 py-1.5 text-sm font-medium text-white hover:bg-slate-700"
      >
        ✕ Close
      </button>

      {isLoading && (
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-slate-700 border-t-white" />
      )}

      {!isLoading && (error || !playback) && (
        <p className="text-red-400">Failed to load playback info.</p>
      )}

      {!isLoading && playback && videoSrc && (
        <video
          className="max-h-full max-w-full"
          controls
          autoPlay
          src={videoSrc}
          onEnded={() => setHasEnded(true)}
        >
          {playback.subtitles &&
            playback.subtitles.map((track) => (
              <track
                key={track.id}
                kind="subtitles"
                src={`${MEDIA_URL_ROOT}${track.filePath}`}
                srcLang={track.languageCode}
                label={track.label}
                default={track.isDefault}
              />
            ))}
        </video>
      )}

      {/* Up next — episodes only, shown once playback ends */}
      {mediaType === "EPISODE" && hasEnded && nextEpisode && (
        <div className="absolute bottom-10 right-10 flex w-80 gap-3 rounded-md bg-slate-900/95 p-4 shadow-lg">
          <div className="aspect-video w-28 shrink-0 overflow-hidden rounded bg-slate-800">
            {nextEpisode.stillPath && (
              <img
                src={`${MEDIA_URL_ROOT}${nextEpisode.stillPath}`}
                alt={nextEpisode.title}
                className="h-full w-full object-cover"
              />
            )}
          </div>
          <div className="min-w-0 flex-1">
            <p className="text-xs text-slate-400">Up next</p>
            <p className="truncate text-sm font-medium text-white">
              {nextEpisode.episodeNumber}. {nextEpisode.title}
            </p>
            <button
              onClick={handlePlayNext}
              className="mt-2 rounded-md bg-white px-3 py-1.5 text-xs font-medium text-black hover:bg-slate-200"
            >
              Play next episode
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
