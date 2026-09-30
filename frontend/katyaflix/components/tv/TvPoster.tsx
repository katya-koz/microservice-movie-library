"use client";

import Link from "next/link";

export type TvMediaItem = {
  id: string;
  title: string;
  posterPath?: string | null;
  type: "MOVIE" | "SHOW";
};

export default function TvPoster({
  item,
  autoFocus,
}: {
  item: TvMediaItem;
  autoFocus?: boolean;
}) {
  const base = item.type === "MOVIE" ? "movies" : "shows";

  return (
    <Link
      href={`/tv/${base}/${item.id}`}
      data-tv-focusable
      data-tv-autofocus={autoFocus ? "" : undefined}
      className="tv-focusable block w-52 shrink-0 rounded-xl"
    >
      <div className="aspect-[2/3] overflow-hidden rounded-xl bg-slate-800">
        {item.posterPath ? (
          <img
            src={item.posterPath}
            alt=""
            className="h-full w-full object-cover"
          />
        ) : (
          <div className="flex h-full items-center justify-center p-4 text-center text-xl text-slate-500">
            {item.title}
          </div>
        )}
      </div>
      <p className="mt-3 truncate px-1 text-center text-xl text-slate-200">
        {item.title}
      </p>
    </Link>
  );
}
