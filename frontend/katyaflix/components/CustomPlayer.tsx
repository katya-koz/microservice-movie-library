"use client";

import { useMemo } from "react";
import { useMoviePlayback } from "@/hooks/useMoviePlayback";

const MEDIA_URL_ROOT =
  process.env.NEXT_PUBLIC_MEDIA_URL_ROOT ?? "http://localhost:8081/media";

type CustomPlayerProps = {
  movieId: string;
  open: boolean;
  onClose: () => void;
};

export default function CustomPlayer({
  movieId,
  open,
  onClose,
}: CustomPlayerProps) {
  const { data: playback, isLoading, error } = useMoviePlayback(movieId, open);

  const videoSrc = useMemo(
    () => (playback ? `${MEDIA_URL_ROOT}${playback.filePath}` : null),
    [playback],
  );

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[200] flex items-center justify-center bg-black/90">
      <button
        onClick={onClose}
        className="absolute right-6 top-6 rounded-full bg-slate-800/80 px-3 py-1.5 text-sm font-medium text-white hover:bg-slate-700"
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
        >
          {playback.subtitles.map((track) => (
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
    </div>
  );
}
