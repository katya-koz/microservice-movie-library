"use client";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useUser } from "@/context/UserContext";
import { Check, PlayFill, Plus } from "react-bootstrap-icons";
import {
  useAddToWatchlist,
  useIsOnWatchlist,
  useRemoveFromWatchlist,
} from "@/hooks/useWatchlist";
import { useResolveShowPlayback } from "@/hooks/usePlayback";

type DetailProps = {
  title: string | null;
  backdropPath: string | null;
  subtitle: React.ReactNode | null; // span element
  overview: string | null;
  genres: string[] | null;
  isLoading: boolean;
  error: Error | null;
  type: "MOVIE" | "SHOW";
  id: string;
  extraContent?: React.ReactNode | null; // rendered below the regular detail page, e.g. seasons/episodes for a SHOW
};

const MEDIA_URL_ROOT =
  process.env.NEXT_PUBLIC_MEDIA_URL_ROOT ?? "http://localhost:8081/media";

export default function DetailPage({
  title,
  backdropPath,
  subtitle,
  overview,
  genres,
  isLoading,
  error,
  type,
  id,
  extraContent,
}: DetailProps) {
  const router = useRouter();
  const { user } = useUser();

  const isOnList = useIsOnWatchlist(user?.id, type, id);
  const addToWatchlist = useAddToWatchlist(user?.id);
  const removeFromWatchlist = useRemoveFromWatchlist(user?.id);
  const resolveShowPlayback = useResolveShowPlayback(user?.id);

  const [backdropOpacity, setBackdropOpacity] = useState(1);

  useEffect(() => {
    const handleScroll = () => {
      const fadeDistance = 400;

      const opacity = Math.max(0, 1 - window.scrollY / fadeDistance);

      setBackdropOpacity(opacity);
    };

    window.addEventListener("scroll", handleScroll, { passive: true });

    return () => {
      window.removeEventListener("scroll", handleScroll);
    };
  }, []);

  if (isLoading) {
    return (
      <div className={"detail-overlay"}>
        <div className="flex min-h-screen items-center justify-center">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-slate-700 border-t-white" />
        </div>
      </div>
    );
  }

  if (error || !title) {
    return (
      <div className={"detail-overlay"}>
        <div className="flex min-h-screen flex-col items-center justify-center">
          <p className="text-red-400">Failed to load media.</p>
        </div>
      </div>
    );
  }

  const handlePlay = () => {
    if (type === "MOVIE") {
      router.push(`/watch/movie?movie_id=${id}`);
      return;
    }

    // SHOW: ask the userservice where this show was left off, then decide
    // next-episode vs resume-current based on how much of it is watched.
    if (!user) return;

    resolveShowPlayback.mutate(id, {
      onSuccess: (target) => {
        if (target) {
          router.push(target);
        }
        // target is null when there's no watch history for this show yet
        // (see the TODO in usePlayback.ts — season-1-episode-1 fallback
        // isn't wired up yet).
      },
      onError: (err) => {
        console.error("Failed to resolve playback target", err);
      },
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
    <div className="flex flex-col w-full h-full bg-slate-900">
      <div className="sticky top-0 h-[50vh] shrink-0">
        {backdropPath && (
          <div
            className="absolute 
            inset-0 
            overflow-hidden
            rounded-2xl
            pointer-events-none
            before:absolute
            before:inset-0
            before:z-10
            before:pointer-events-none
            before:content-['']
            before:bg-[radial-gradient(circle,rgba(255,255,255,0)_40%,theme(colors.slate.900)_90%),linear-gradient(90deg,theme(colors.slate.900)_0%,rgba(255,255,255,0)_30%,rgba(255,255,255,0)_70%,theme(colors.slate.900)_100%),linear-gradient(180deg,rgba(255,255,255,0)_50%,theme(colors.slate.900)_97%)]
            before:bg-cover
            "
          >
            <img
              src={backdropPath}
              alt=""
              className="h-full w-full object-cover object-top overflow-hidden "
            />
          </div>
        )}
      </div>

      {/* Details */}
      <div className="relative px-8 pb-8 z-100 bg-[linear-gradient(to_bottom,transparent_0%,theme(colors.slate.900)_10%,theme(colors.slate.900)_100%)]">
        <div className="-mt-24">
          <div className="max-w-3xl">
            <h1 className="text-3xl font-bold text-white">{title}</h1>

            <div className="mt-3 flex flex-wrap items-center gap-2 text-slate-400">
              {subtitle}
            </div>

            {overview && (
              <p className="mt-6 text-lg leading-7 text-slate-300">
                {overview}
              </p>
            )}
            <div className="flex gap-2 mt-4">
              {genres &&
                genres.map((g, i) => (
                  <div
                    className="flex rounded-full p-2 pl-4 pr-4 border-2 border-green-400 text-green-400"
                    key={i}
                  >
                    {g}
                  </div>
                ))}
            </div>

            {/* Actions */}
            <div className="mt-8 flex gap-3 ">
              <button
                onClick={handlePlay}
                disabled={type === "SHOW" && resolveShowPlayback.isPending}
                className="flex items-center gap-2 rounded-full bg-white p-2 pr-6 text-base mb-6 text-black transition hover:bg-slate-200 disabled:opacity-50"
              >
                <PlayFill size={40} />
                Play
              </button>

              <button
                onClick={handleToggleList}
                disabled={!user}
                className="flex items-center gap-2 rounded-full bg-slate-800 p-2 pr-6 text-base mb-6 text-white transition hover:bg-slate-700 disabled:opacity-50"
              >
                {isOnList ? <Check size={40} /> : <Plus size={40} />}
                My List
              </button>
            </div>
          </div>
          {extraContent}
        </div>
      </div>
    </div>
  );
}
