"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useShowSeasons } from "@/hooks/useShowSeasons";
import { useSeasonEpisodes } from "@/hooks/useShowEpisodes";

const MEDIA_URL_ROOT =
  process.env.NEXT_PUBLIC_MEDIA_URL_ROOT ?? "http://localhost:8081/media";

function mediaUrl(path: string | null) {
  return path ? `${MEDIA_URL_ROOT}${path}` : null;
}

function formatRuntime(minutes: number | null) {
  if (!minutes) return null;
  return `${minutes}m`;
}

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

type SeasonEpisodesSectionProps = {
  showId: string;
};

export default function SeasonEpisodesSection({
  showId,
}: SeasonEpisodesSectionProps) {
  const router = useRouter();

  const {
    data: seasons,
    isLoading: seasonsLoading,
    error: seasonsError,
  } = useShowSeasons(showId);

  const [selectedSeasonNumber, setSelectedSeasonNumber] = useState<
    number | null
  >(null);

  // Default to the first season once seasons load.
  useEffect(() => {
    if (seasons && seasons.length > 0 && selectedSeasonNumber === null) {
      setSelectedSeasonNumber(seasons[0].seasonNumber);
    }
  }, [seasons, selectedSeasonNumber]);

  const selectedSeason = useMemo(
    () => seasons?.find((s) => s.seasonNumber === selectedSeasonNumber),
    [seasons, selectedSeasonNumber],
  );

  const {
    data: episodes,
    isLoading: episodesLoading,
    error: episodesError,
  } = useSeasonEpisodes(showId, selectedSeasonNumber);

  const handlePlayEpisode = (episodeId: string) => {
    router.push(`/watch/episode?episode_id=${episodeId}`);
  };

  if (seasonsError) {
    return (
      <div className="px-8 pb-8">
        <p className="text-sm text-red-400">Failed to load seasons.</p>
      </div>
    );
  }

  if (!seasonsLoading && (!seasons || seasons.length === 0)) {
    return null;
  }

  return (
    <div className="border-t border-slate-800 px-8 py-8">
      <div className="flex flex-col gap-8 lg:flex-row">
        {/* Left: season selector + poster */}
        <div className="w-full shrink-0 lg:w-56">
          {seasonsLoading ? (
            <div className="h-10 w-full animate-pulse rounded-md bg-slate-800" />
          ) : (
            <select
              value={selectedSeasonNumber ?? ""}
              onChange={(e) => setSelectedSeasonNumber(Number(e.target.value))}
              className="w-full rounded-md border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-white focus:border-slate-500 focus:outline-none"
            >
              {seasons?.map((season) => (
                <option key={season.seasonId} value={season.seasonNumber}>
                  {season.title}
                </option>
              ))}
            </select>
          )}

          <div className="mt-4 aspect-[2/3] w-full overflow-hidden rounded-md bg-slate-800">
            {selectedSeason?.posterPath && (
              <img
                src={mediaUrl(selectedSeason.posterPath) ?? undefined}
                alt={selectedSeason.title}
                className="h-full w-full object-cover"
              />
            )}
          </div>
        </div>

        {/* Right: episode list */}
        <div className="min-w-0 flex-1">
          {episodesError && (
            <p className="text-sm text-red-400">Failed to load episodes.</p>
          )}

          {episodesLoading && (
            <div className="flex flex-col gap-4">
              {Array.from({ length: 3 }).map((_, i) => (
                <div key={i} className="flex gap-4">
                  <div className="aspect-video w-40 shrink-0 animate-pulse rounded-md bg-slate-800" />
                  <div className="flex-1 space-y-2 py-1">
                    <div className="h-4 w-1/3 animate-pulse rounded bg-slate-800" />
                    <div className="h-3 w-full animate-pulse rounded bg-slate-800" />
                    <div className="h-3 w-2/3 animate-pulse rounded bg-slate-800" />
                  </div>
                </div>
              ))}
            </div>
          )}

          {!episodesLoading && episodes && episodes.length === 0 && (
            <p className="text-sm text-slate-400">
              No episodes available for this season.
            </p>
          )}

          {!episodesLoading && episodes && episodes.length > 0 && (
            <div className="divide-y divide-slate-800">
              {episodes.map((episode) => {
                const runtime = formatRuntime(episode.runtimeMinutes);
                const airDate = formatAirDate(episode.airDate);
                const meta = [runtime, airDate].filter(Boolean).join(" • ");

                return (
                  <button
                    key={episode.id}
                    onClick={() => handlePlayEpisode(episode.id)}
                    className="group flex w-full gap-4 rounded-md px-2 py-4 text-left transition hover:bg-slate-800/40"
                  >
                    <div className="relative aspect-video w-40 shrink-0 overflow-hidden rounded-md bg-slate-800">
                      {episode.stillPath && (
                        <img
                          src={mediaUrl(episode.stillPath) ?? undefined}
                          alt={episode.title}
                          className="h-full w-full object-cover"
                        />
                      )}
                      <div className="absolute inset-0 flex items-center justify-center bg-black/0 transition group-hover:bg-black/40">
                        <span className="text-2xl text-white opacity-0 transition group-hover:opacity-100">
                          ▶
                        </span>
                      </div>
                    </div>

                    <div className="min-w-0 flex-1">
                      <div className="flex items-baseline justify-between gap-3">
                        <h3 className="truncate font-medium text-white">
                          {episode.episodeNumber}. {episode.title}
                        </h3>
                        {meta && (
                          <span className="shrink-0 text-xs text-slate-400">
                            {meta}
                          </span>
                        )}
                      </div>
                      {episode.overview && (
                        <p className="mt-1 line-clamp-2 text-sm text-slate-400">
                          {episode.overview}
                        </p>
                      )}
                    </div>
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
