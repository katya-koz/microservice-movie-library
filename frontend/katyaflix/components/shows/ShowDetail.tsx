"use client";

import { useShowDetail } from "@/hooks/useShowDetail";
import { useUser } from "@/context/UserContext";
import DetailPage from "../media/DetailPage";
import SeasonEpisodesSection from "./SeasonEpisodesSection";
type ShowDetailProps = {
  showId: string;
};

export default function ShowDetail({ showId }: ShowDetailProps) {
  const { data: show, isLoading, error } = useShowDetail(showId);
  const { user } = useUser();
  if (isLoading) {
    return (
      <DetailPage
        isLoading={true}
        title={null}
        backdropPath={null}
        subtitle={undefined}
        overview={null}
        genres={null}
        error={null}
        type="SHOW"
        id={"null_id"}
      />
    );
  }

  if (error || !show) {
    return (
      <DetailPage
        isLoading={false}
        error={error}
        title={null}
        backdropPath={null}
        subtitle={undefined}
        overview={null}
        genres={null}
        type="SHOW"
        id={"null_id"}
      />
    );
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
      genres={show.genres}
      type="SHOW"
      id={show.id}
      extraContent={
        <SeasonEpisodesSection
          userId={user?.id ?? "no_user"}
          showId={show.id}
        />
      }
    />
  );
}
