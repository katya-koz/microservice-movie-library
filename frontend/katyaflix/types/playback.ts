export type SubtitleTrack = {
  id: string;
  languageCode: string;
  label: string;
  filePath: string;
  isForced: boolean;
  isSdh: boolean;
  isDefault: boolean;
};

export type PlaybackInfo = {
  mediaFileId: string;
  filePath: string;
  containerFormat: string;
  durationSeconds: number;
  resolution: string;
  subtitles: SubtitleTrack[];

  seasonId: string | null;
  showId: string | null;
};

// Full metadata for the "up next" preview card in the player (title, still,
// episode/season numbers). Used by useNextEpisode.
export type NextEpisode = {
  episodeId: string;
  showId: string;
  episodeNumber: number;
  seasonNumber: number;
  title: string;
  stillPath: string | null;
};

// Just enough to build a /watch/episode URL. The /next endpoint returns the
// NextEpisode shape above (it has no seasonId), so this is just the ids the
// show page's Play button needs to navigate.
export type NextEpisodeRef = Pick<NextEpisode, "episodeId" | "showId">;
