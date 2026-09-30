"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useShowSeasons } from "@/hooks/useShowSeasons";
import { useSeasonEpisodes } from "@/hooks/useShowEpisodes";
import { useEpisodeWatchtimes } from "@/hooks/useWatchtime";

function formatAirDate(dateString: string | null) {
  if (!dateString) return null;
  const date = new Date(dateString);
  if (Number.isNaN(date.getTime())) return null;
  return date.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

export default function TvSeasonEpisodes({
  showId,
  userId,
}: {
  showId: string;
  userId: string;
}) {
  const router = useRouter();
  const { data: seasons, isLoading: seasonsLoading } = useShowSeasons(showId);
  const [selected, setSelected] = useState<number | null>(null);

  useEffect(() => {
    if (seasons && seasons.length > 0 && selected === null) {
      setSelected(seasons[0].seasonNumber);
    }
  }, [seasons, selected]);

  const { data: episodes, isLoading: episodesLoading } = useSeasonEpisodes(
    showId,
    selected,
  );
  const episodeIds = useMemo(() => episodes?.map((e) => e.id), [episodes]);
  const { data: watchtimes } = useEpisodeWatchtimes(userId, episodeIds);

  const progressById = useMemo(() => {
    const map = new Map<string, number>();
    watchtimes?.forEach((wt) => {
      if (wt.mediaType === "EPISODE" && wt.durationSeconds > 0) {
        map.set(
          wt.mediaId,
          Math.min(1, Math.max(0, wt.watchtimeSeconds / wt.durationSeconds)),
        );
      }
    });
    return map;
  }, [watchtimes]);

  if (!seasonsLoading && (!seasons || seasons.length === 0)) return null;

  return (
    <section className="px-[4vw] pb-24 pt-4">
      <div className="tv-scroll mb-8 flex gap-4 overflow-x-auto py-3">
        {seasons?.map((season) => (
          <button
            key={season.seasonId}
            data-tv-focusable
            onClick={() => setSelected(season.seasonNumber)}
            className={`tv-focusable tv-flat shrink-0 rounded-full px-8 py-3 text-2xl ${
              season.seasonNumber === selected
                ? "bg-white text-black"
                : "bg-slate-800 text-slate-200"
            }`}
          >
            {season.title}
          </button>
        ))}
      </div>

      {episodesLoading && (
        <div className="flex flex-col gap-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-40 animate-pulse rounded-xl bg-slate-800" />
          ))}
        </div>
      )}

      {!episodesLoading && episodes && episodes.length === 0 && (
        <p className="text-2xl text-slate-400">
          No episodes available for this season.
        </p>
      )}

      <div className="flex flex-col gap-4">
        {episodes?.map((episode) => {
          const progress = progressById.get(episode.id);
          const meta = [
            episode.runtimeMinutes ? `${episode.runtimeMinutes}m` : null,
            formatAirDate(episode.airDate),
          ]
            .filter(Boolean)
            .join(" • ");

          return (
            <button
              key={episode.id}
              data-tv-focusable
              onClick={() => router.push(`/watch/episode?episode_id=${episode.id}`)}
              className="tv-focusable flex w-full gap-6 rounded-xl bg-slate-900 p-4 text-left"
            >
              <div className="relative aspect-video w-72 shrink-0 overflow-hidden rounded-lg bg-slate-800">
                {episode.stillPath && (
                  <img
                    src={episode.stillPath}
                    alt=""
                    className="h-full w-full object-cover"
                  />
                )}
                {progress !== undefined && (
                  <div className="absolute inset-x-0 bottom-0 h-1.5 bg-black/60">
                    <div
                      className="h-full bg-green-500"
                      style={{ width: `${progress * 100}%` }}
                    />
                  </div>
                )}
              </div>

              <div className="min-w-0 flex-1 py-1">
                <div className="flex items-baseline justify-between gap-4">
                  <h3 className="truncate text-3xl font-medium">
                    {episode.episodeNumber}. {episode.title}
                  </h3>
                  {meta && (
                    <span className="shrink-0 text-xl text-slate-400">{meta}</span>
                  )}
                </div>
                {episode.overview && (
                  <p className="mt-2 line-clamp-2 text-xl text-slate-400">
                    {episode.overview}
                  </p>
                )}
              </div>
            </button>
          );
        })}
      </div>
    </section>
  );
}
