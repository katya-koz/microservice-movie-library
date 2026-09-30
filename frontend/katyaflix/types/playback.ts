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
  episodeNumber: number;
  seasonNumber: number;
  title: string;
  stillPath: string | null;
};

// Just enough to build a /watch/episode URL. This is intentionally NOT the
// same as NextEpisode — resolveShowPlaybackTarget (the show page's Play
// button) only needs ids to navigate, not display metadata, and hits a
// different endpoint than the player's "up next" card does. Renamed from
// the old "NextEpisode2" and exported (it wasn't before, which meant
// api/playback.ts couldn't legally type its return value with it).
export type NextEpisodeRef = {
  episodeId: string;
  seasonId: string;
  showId: string;
};
