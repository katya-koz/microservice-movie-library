"use client";

import { useShowDetail } from "@/hooks/useShowDetail";
import { useUser } from "@/context/UserContext";
import TvDetail from "./TvDetail";
import TvSeasonEpisodes from "./TvSeasonEpisodes";
import { TvMessage, TvSpinner } from "./TvStatus";

export default function TvShowDetail({ showId }: { showId: string }) {
  const { data: show, isLoading, error } = useShowDetail(showId);
  const { user } = useUser();

  if (isLoading) return <TvSpinner />;
  if (error || !show) return <TvMessage>Failed to load this show.</TvMessage>;

  const year = show.firstAirDate ? new Date(show.firstAirDate).getFullYear() : null;

  const subtitle = (
    <>
      {year && <span>{year}</span>}
      {show.status && <span>• {show.status}</span>}
      {show.creatorNames && (
        <span>
          • Created by <span className="text-white">{show.creatorNames}</span>
        </span>
      )}
    </>
  );

  return (
    <TvDetail
      title={show.title}
      backdropPath={show.backdropPath}
      subtitle={subtitle}
      overview={show.overview}
      genres={show.genres ?? []}
      type="SHOW"
      id={show.id}
      extraContent={
        user ? <TvSeasonEpisodes showId={show.id} userId={user.id} /> : null
      }
    />
  );
}
