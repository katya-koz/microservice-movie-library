"use client";

import Link from "next/link";
import { MovieSummary } from "@/types/movie";
import { ShowSummary } from "@/types/show";

interface PosterProps {
  summary: MovieSummary | ShowSummary;
  type: "MOVIE" | "SHOW";
}

export default function Poster({ summary, type }: PosterProps) {
  const reference = type == "MOVIE" ? "movies" : "shows";

  return (
    <Link
      key={summary.id}
      href={`/${reference}/${summary.id}/detail`}
      className="group"
    >
      <div key={summary.id}>
        <div className="aspect-[2/3] overflow-hidden rounded-lg bg-slate-800">
          {summary.posterPath && (
            <img
              src={
                process.env.NEXT_PUBLIC_MEDIA_URL_ROOT +
                "/" +
                summary.posterPath
              }
              alt={summary.title}
              className="h-full w-full object-cover"
            />
          )}
        </div>

        <h2 className="mt-2 text-sm font-medium">{summary.title}</h2>
      </div>
    </Link>
  );
}
