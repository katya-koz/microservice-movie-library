"use client";

import { useRouter } from "next/navigation";
import { useMovieDetail } from "@/hooks/useMovieDetail";

type DetailProps = {
  title: string;
  backdropPath: string;
  subtitle: React.ReactNode; // span element
  overview: string;
  isLoading: boolean;
  error: Error;
  type: "MOVIE" | "SHOW";
  id: string;
  extraContent?: React.ReactNode; // rendered below the regular detail page, e.g. seasons/episodes for a SHOW
};

const MEDIA_URL_ROOT =
  process.env.NEXT_PUBLIC_MEDIA_URL_ROOT ?? "http://localhost:8081/media";

export default function DetailPage({
  title,
  backdropPath,
  subtitle,
  overview,
  isLoading,
  error,
  type,
  id,
  extraContent,
}: DetailProps) {
  const router = useRouter();

  //   const { data: movie, isLoading, error } = useMovieDetail(movieId);

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

  const backdrop = backdropPath ? `${MEDIA_URL_ROOT}${backdropPath}` : null;

  const handlePlay = () => {
    if (type === "MOVIE") {
      router.push(`/watch/movie?movie_id=${id}`);
    }
    // TODO: SHOW playback route: /watch/episode?episode_id=...
  };

  return (
    <div className="w-full h-full bg-slate-900">
      {/* Backdrop */}
      <div
        className="relative h-3/5 justify-items-center
"
      >
        {backdrop && (
          <div
            className="
              absolute inset-0
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
              src={backdrop}
              alt=""
              className="h-full w-full object-cover object-top overflow-hidden "
            />
          </div>
        )}
        <div className="absolute inset-0 " /> {}
      </div>

      {/* Details */}
      <div className="relative -mt-24 px-8 pb-8 z-100">
        <div className="max-w-3xl">
          <h1 className="text-4xl font-bold text-white">{title}</h1>

          <div className="mt-3 flex flex-wrap items-center gap-2 text-sm text-slate-400">
            {subtitle}
          </div>

          {overview && (
            <p className="mt-6 text-base leading-7 text-slate-300">
              {overview}
            </p>
          )}

          {/* Actions */}
          <div className="mt-8 flex gap-3">
            <button
              onClick={handlePlay}
              className="flex items-center gap-2 rounded-md bg-white px-5 py-2.5 font-medium text-black transition hover:bg-slate-200"
            >
              <span>▶</span>
              Play
            </button>

            <button className="flex items-center gap-2 rounded-md bg-slate-800 px-5 py-2.5 font-medium text-white transition hover:bg-slate-700">
              <span>＋</span>
              My List
            </button>
          </div>
        </div>
      </div>

      {/* Optional content rendered below the regular detail page (e.g. seasons/episodes for a SHOW) */}
      {extraContent}
    </div>
  );
}
