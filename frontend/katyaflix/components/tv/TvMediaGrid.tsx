"use client";

import Link from "next/link";
import TvPoster, { TvMediaItem } from "./TvPoster";

export default function TvMediaGrid({
  title,
  items,
  totalElements,
  totalPages,
  page,
  loading,
  error,
  onPageChange,
}: {
  title: string;
  items: TvMediaItem[];
  totalElements: number;
  totalPages: number;
  page: number;
  loading: boolean;
  error?: Error | null;
  onPageChange: (page: number) => void;
}) {
  return (
    <div data-tv-content className="px-[4vw] pb-20 pt-28">
      <div className="mb-10 flex items-center justify-between">
        <h1 className="text-6xl font-bold">{title}</h1>
        <span className="text-2xl text-slate-500">
          {totalElements} {totalElements === 1 ? "item" : "items"}
        </span>
      </div>

      {loading && (
        <div className="grid grid-cols-6 gap-x-6 gap-y-10">
          {Array.from({ length: 12 }).map((_, i) => (
            <div
              key={i}
              className="aspect-[2/3] animate-pulse rounded-xl bg-slate-800"
            />
          ))}
        </div>
      )}

      {!loading && error && (
        <p className="text-3xl text-red-400">Failed to load {title.toLowerCase()}.</p>
      )}

      {!loading && !error && items.length === 0 && (
        <div className="flex flex-col items-start gap-6">
          <p className="text-3xl text-slate-400">Nothing here yet.</p>
          <Link
            href="/tv/search"
            data-tv-focusable
            className="tv-focusable rounded-full bg-slate-800 px-8 py-3 text-2xl"
          >
            Search titles
          </Link>
        </div>
      )}

      {!loading && !error && items.length > 0 && (
        <div className="grid grid-cols-6 gap-x-6 gap-y-10">
          {items.map((item, i) => (
            <div key={`${item.type}-${item.id}`} className="flex justify-center">
              <TvPoster item={item} autoFocus={i === 0} />
            </div>
          ))}
        </div>
      )}

      {!loading && totalPages > 1 && (
        <div className="mt-14 flex items-center justify-center gap-8">
          <button
            data-tv-focusable
            disabled={page <= 1}
            onClick={() => onPageChange(page - 1)}
            className="tv-focusable rounded-full bg-slate-800 px-10 py-3 text-2xl disabled:opacity-30"
          >
            Previous
          </button>
          <span className="text-2xl text-slate-400">
            {page} / {totalPages}
          </span>
          <button
            data-tv-focusable
            disabled={page >= totalPages}
            onClick={() => onPageChange(page + 1)}
            className="tv-focusable rounded-full bg-slate-800 px-10 py-3 text-2xl disabled:opacity-30"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
