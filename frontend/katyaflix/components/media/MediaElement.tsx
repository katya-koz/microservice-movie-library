import Link from "next/link";
import { MovieSummary } from "@/types/movie";
import { ShowSummary } from "@/types/show";

type MediaElementProps = {
  media: MovieSummary | ShowSummary;
  mediaType: "movie" | "show";
};

export default function MediaElement({ media, mediaType }: MediaElementProps) {
  const href =
    mediaType === "movie"
      ? `/movies/${media.id}/detail`
      : `/shows/${media.id}/detail`;

  const year = media.releaseDate
    ? new Date(media.releaseDate).getFullYear()
    : null;

  return (
    <Link href={href} className="group block">
      {/* Poster */}
      <div className="aspect-[2/3] overflow-hidden rounded-lg bg-slate-800">
        {media.posterPath ? (
          <img
            src={media.posterPath}
            alt={media.title}
            className="h-full w-full object-cover transition-transform duration-200 group-hover:scale-105"
          />
        ) : (
          <div className="flex h-full items-center justify-center">
            <span className="px-4 text-center text-sm text-slate-600">
              No poster
            </span>
          </div>
        )}
      </div>

      {/* Info */}
      <div className="mt-2">
        <h2 className="truncate text-sm font-medium text-white">
          {media.title}
        </h2>

        {year && <p className="text-xs text-slate-500">{year}</p>}
      </div>
    </Link>
  );
}
