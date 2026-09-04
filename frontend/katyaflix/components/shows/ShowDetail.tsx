"use client";

import { useShowDetail } from "@/hooks/useShowDetail";
import DetailPage from "../media/DetailPage";
import SeasonEpisodesSection from "./SeasonEpisodesSection";
type ShowDetailProps = {
  showId: string;
  modal?: boolean;
};

export default function ShowDetail({ showId, modal = false }: ShowDetailProps) {
  const { data: show, isLoading, error } = useShowDetail(showId);

  if (isLoading) {
    return <DetailPage isLoading={true} />;
  }

  if (error || !show) {
    return <DetailPage isLoading={false} error={error} />;
  }

  const firstAirYear = show.firstAirDate
    ? new Date(show.firstAirDate).getFullYear()
    : null;

  const subtitle = (
    <div className="mt-3 flex flex-wrap items-center gap-2 text-sm text-zinc-400">
      {firstAirYear && <span>{firstAirYear}</span>}

      {show.status && (
        <>
          <span>•</span>
          <span>{show.status}</span>
        </>
      )}

      {show.creatorNames && (
        <>
          <span>•</span>
          <span>
            Created by{" "}
            <span className="text-zinc-200">{show.creatorNames}</span>
          </span>
        </>
      )}
    </div>
  );

  return (
    <DetailPage
      title={show.title}
      backdropPath={show.backdropPath}
      subtitle={subtitle}
      overview={show.overview}
      isLoading={isLoading}
      error={error}
      type="SHOW"
      id={show.id}
      extraContent={<SeasonEpisodesSection showId={show.id} />}
    />
  );
}
