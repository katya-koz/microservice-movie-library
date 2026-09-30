import { MediaType } from "@/types/media";

// Raw watchlist row as the user-service returns it (no title/poster — just
// enough to check membership and to look up catalog summaries for).
export type WatchlistItem = {
  id: string;
  mediaType: MediaType;
  mediaId: string;
  added: string;
};
