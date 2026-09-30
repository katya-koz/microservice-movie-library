"use client";

import TvPoster, { TvMediaItem } from "./TvPoster";

export default function TvRow({
  title,
  items,
  isLoading,
  gutter = "px-[4vw]",
  autoFocusFirst,
}: {
  title: string;
  items: TvMediaItem[];
  isLoading?: boolean;
  gutter?: string;
  autoFocusFirst?: boolean;
}) {
  if (!isLoading && items.length === 0) return null;

  return (
    <section>
      <h2 className={`${gutter} text-4xl font-bold`}>{title}</h2>
      <div className={`tv-scroll flex gap-6 overflow-x-auto py-8 ${gutter}`}>
        {isLoading
          ? Array.from({ length: 8 }).map((_, i) => (
              <div
                key={i}
                className="aspect-[2/3] w-52 shrink-0 animate-pulse rounded-xl bg-slate-800"
              />
            ))
          : items.map((item, i) => (
              <TvPoster
                key={`${item.type}-${item.id}`}
                item={item}
                autoFocus={autoFocusFirst && i === 0}
              />
            ))}
      </div>
    </section>
  );
}
