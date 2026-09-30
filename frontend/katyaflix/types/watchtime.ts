export type WatchtimeMediaType = "MOVIE" | "EPISODE";

export type Watchtime = {
  mediaType: WatchtimeMediaType;
  mediaId: string;
  watchtimeSeconds: number;
  // SeasonEpisodesSection reads this to compute each episode's progress bar
  // (watchtimeSeconds / durationSeconds), so it has to be here. One of the
  // two old duplicate hook files was missing this field — that was a bug,
  // not an intentional slimmer variant.
  durationSeconds: number;
  updatedAt: string | null;
};
