"use client";

import { ReactNode } from "react";
import { useRouter } from "next/navigation";
import { Check, PlayFill, Plus } from "react-bootstrap-icons";
import { useUser } from "@/context/UserContext";
import {
  useAddToWatchlist,
  useIsOnWatchlist,
  useRemoveFromWatchlist,
} from "@/hooks/useWatchlist";
import { useResolveShowPlayback } from "@/hooks/usePlayback";

type TvDetailProps = {
  title: string;
  backdropPath?: string | null;
  subtitle: ReactNode;
  overview?: string | null;
  genres: string[];
  type: "MOVIE" | "SHOW";
  id: string;
  extraContent?: ReactNode;
};

export default function TvDetail({
  title,
  backdropPath,
  subtitle,
  overview,
  genres,
  type,
  id,
  extraContent,
}: TvDetailProps) {
  const router = useRouter();
  const { user } = useUser();

  const isOnList = useIsOnWatchlist(user?.id, type, id);
  const addToWatchlist = useAddToWatchlist(user?.id);
  const removeFromWatchlist = useRemoveFromWatchlist(user?.id);
  const resolveShowPlayback = useResolveShowPlayback(user?.id);

  const handlePlay = () => {
    if (type === "MOVIE") {
      router.push(`/watch/movie?movie_id=${id}`);
      return;
    }
    if (!user) return;
    resolveShowPlayback.mutate(id, {
      onSuccess: (target) => {
        if (target) router.push(target);
      },
      onError: (err) => console.error("Failed to resolve playback target", err),
    });
  };

  const handleToggleList = () => {
    if (!user) return;
    if (isOnList) {
      removeFromWatchlist.mutate({ mediaType: type, mediaId: id });
    } else {
      addToWatchlist.mutate({ mediaType: type, mediaId: id });
    }
  };

  return (
    <div>
      {/* Hero */}
      <section className="relative h-screen">
        {backdropPath && (
          <img
            src={backdropPath}
            alt=""
            className="absolute inset-0 h-full w-full object-cover object-top"
          />
        )}
        <div className="absolute inset-0 bg-gradient-to-r from-slate-950 via-slate-950/70 to-transparent" />
        <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-transparent to-slate-950/40" />

        <div className="absolute bottom-[12vh] left-[4vw] max-w-[52vw]">
          <h1 className="text-7xl font-bold leading-tight">{title}</h1>

          <div className="mt-4 flex flex-wrap items-center gap-3 text-2xl text-slate-300">
            {subtitle}
          </div>

          {overview && (
            <p className="mt-6 line-clamp-4 text-2xl leading-relaxed text-slate-200">
              {overview}
            </p>
          )}

          <div className="mt-6 flex flex-wrap gap-3">
            {genres.map((g) => (
              <span
                key={g}
                className="rounded-full border-2 border-green-400 px-5 py-1.5 text-xl text-green-400"
              >
                {g}
              </span>
            ))}
          </div>

          <div className="mt-10 flex gap-5">
            <button
              data-tv-focusable
              data-tv-autofocus
              onClick={handlePlay}
              disabled={type === "SHOW" && resolveShowPlayback.isPending}
              className="tv-focusable flex items-center gap-3 rounded-full bg-white py-2 pl-4 pr-10 text-3xl font-medium text-black disabled:opacity-60"
            >
              <PlayFill size={52} />
              Play
            </button>

            <button
              data-tv-focusable
              onClick={handleToggleList}
              disabled={!user}
              className="tv-focusable flex items-center gap-3 rounded-full bg-slate-800 py-2 pl-4 pr-10 text-3xl font-medium text-white disabled:opacity-60"
            >
              {isOnList ? <Check size={52} /> : <Plus size={52} />}
              My List
            </button>
          </div>
        </div>
      </section>

      {extraContent}
    </div>
  );
}
