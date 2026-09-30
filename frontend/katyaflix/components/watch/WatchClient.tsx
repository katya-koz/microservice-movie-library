"use client";

import { useMemo, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import {
  useMoviePlayback,
  useEpisodePlayback,
  useNextEpisode,
} from "@/hooks/usePlayback";
import { useUser } from "@/context/UserContext";
import { useEpisodeWatchtime, useMovieWatchtime } from "@/hooks/useWatchtime";
import { useWatchtimeSync } from "@/hooks/useWatchtimeSync";
import { CustomVideoPlayer } from "@/components/CustomVideoPlayer";

const MEDIA_URL_ROOT =
  process.env.NEXT_PUBLIC_MEDIA_URL_ROOT ?? "http://localhost:8081/media";

// Don't bother resuming if the viewer was basically at the start, or
// basically at the end (in which case just start over).
const RESUME_MIN_SECONDS = 10;

// The metadata CustomVideoPlayer needs (title, overview, episode/season
// numbers) isn't on the original playback response shape — only `filePath`
// and `subtitles` were. These types document what to add to your
// useMoviePlayback / useEpisodePlayback responses; adjust field names to
// match your actual API.
type MoviePlaybackMeta = {
  filePath: string;
  title: string;
  overview?: string;
  subtitles?: import("@/components/CustomVideoPlayer").SubtitleTrack[];
};
type EpisodePlaybackMeta = {
  filePath: string;
  title: string; // episode title
  showTitle: string;
  showId: string;
  seasonId: string;
  episodeNumber: number;
  seasonNumber: number;
  overview?: string;
  subtitles?: import("@/components/CustomVideoPlayer").SubtitleTrack[];
};

export default function WatchClient() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { user } = useUser();

  const movieId = searchParams.get("movie_id") ?? undefined;
  const episodeId = searchParams.get("episode_id") ?? undefined;
  const mediaType = movieId ? "MOVIE" : episodeId ? "EPISODE" : null;

  const [isPlaying, setIsPlaying] = useState(false);
  const videoRef = useRef<HTMLVideoElement | null>(null);

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

  // Fetched as soon as we know the episode — not gated on the episode
  // having ended — so the player's next-episode button has a preview
  // ready to show on hover throughout playback.
  const { data: nextEpisode } = useNextEpisode(episodeId, {
    enabled: mediaType === "EPISODE" && !!episodeId,
  });

  // Last saved position, so we can resume from where the user left off.
  const movieWatchtime = useMovieWatchtime(
    user?.id,
    mediaType === "MOVIE" ? movieId : undefined,
  );
  const episodeWatchtime = useEpisodeWatchtime(
    user?.id,
    mediaType === "EPISODE" ? episodeId : undefined,
  );
  // Left undefined (not coalesced to 0) until the query actually resolves —
  // CustomVideoPlayer uses "is this defined yet" to know it's safe to seek,
  // so collapsing "not fetched yet" into 0 here would make it commit to a
  // resume position before we actually know it.
  const resumeSeconds = (
    mediaType === "MOVIE" ? movieWatchtime.data : episodeWatchtime.data
  )?.watchtimeSeconds;

  // Split the union into its two branches so the metadata fields below are
  // type-safe instead of relying on inline casts scattered through the JSX.
  // Done ahead of useWatchtimeSync below because that hook needs
  // episodePlayback's showId/seasonId.
  const moviePlayback =
    mediaType === "MOVIE"
      ? (playback as MoviePlaybackMeta | undefined)
      : undefined;
  const episodePlayback =
    mediaType === "EPISODE"
      ? (playback as EpisodePlaybackMeta | undefined)
      : undefined;

  // Periodically PUTs the current playback position (+ total duration) to
  // the user-service over REST, so progress survives a refresh, a closed
  // tab, or a switch to another device. showId/seasonId are only sent for
  // episodes; undefined for movies.
  useWatchtimeSync({
    userId: user?.id,
    mediaType: mediaType === "MOVIE" ? "MOVIE" : "EPISODE",
    mediaId: mediaType === "MOVIE" ? movieId : episodeId,
    showId: episodePlayback?.showId,
    seasonId: episodePlayback?.seasonId,
    getCurrentTimeSeconds: () => videoRef.current?.currentTime ?? 0,
    getDurationSeconds: () => videoRef.current?.duration ?? 0,
    isActive: isPlaying,
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
      {isLoading && (
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-slate-700 border-t-white" />
      )}

      {!isLoading && (error || !playback) && (
        <p className="text-red-400">Failed to load playback info.</p>
      )}

      {!isLoading && playback && videoSrc && (
        <CustomVideoPlayer
          ref={videoRef}
          src={videoSrc}
          mediaUrlRoot={MEDIA_URL_ROOT}
          mediaType={mediaType}
          title={
            mediaType === "MOVIE"
              ? (moviePlayback?.title ?? "")
              : (episodePlayback?.showTitle ?? "")
          }
          episodeTitle={episodePlayback?.title}
          episodeNumber={episodePlayback?.episodeNumber}
          seasonNumber={episodePlayback?.seasonNumber}
          overview={
            mediaType === "MOVIE"
              ? moviePlayback?.overview
              : episodePlayback?.overview
          }
          subtitles={playback.subtitles}
          resumeSeconds={resumeSeconds}
          resumeMinSeconds={RESUME_MIN_SECONDS}
          nextEpisode={nextEpisode}
          onClose={() => router.back()}
          onPlayNext={handlePlayNext}
          onPlayingChange={setIsPlaying}
        />
      )}
    </div>
  );
}
