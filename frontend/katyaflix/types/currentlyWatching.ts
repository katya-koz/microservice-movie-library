import { MediaType } from "@/types/media";

export type CurrentlyWatchingMedia = {
  mediaType: MediaType;
  mediaId: string;
};

export type CurrentEpisode = {
  episodeId: string;
  seasonId: string;
  showId: string;
  watchedSeconds: number;
  durationSeconds: number;
};
