"use client";

import Link from "next/link";
import { MediaSummary } from "@/types/media";

interface PosterProps {
  summary: MediaSummary;
  // type: "MOVIE" | "SHOW";
}

export default function Poster({ summary }: PosterProps) {
  const reference = summary.mediaType == "MOVIE" ? "movies" : "shows";

  return (
    <Link
      key={summary.id}
      href={`/${reference}/${summary.id}/detail`}
      className="group"
    >
      <div key={summary.id}>
        <div
          className="
              z-1
              aspect-[2/3]
              overflow-hidden
              rounded-lg
              outline-slate-800
              bg-slate-800
              transition-all
              duration-200
              ease-out
              hover:scale-[1.03]
              hover:outline
              hover:outline-6
              hover:outline-offset-2
              hover:outline-slate-700
              hover:shadow-xl
            "
        >
          {summary.posterPath && (
            <img
              src={summary.posterPath}
              alt={summary.title}
              className="h-full w-full object-cover"
            />
          )}
        </div>

        <h2 className="mt-2 text-lg text-center font-base ">{summary.title}</h2>
      </div>
    </Link>
  );
}
