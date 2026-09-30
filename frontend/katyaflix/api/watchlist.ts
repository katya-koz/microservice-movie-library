import { API_URL_ROOT } from "@/lib/config";
import { WatchlistItem } from "@/types/watchlist";

export async function fetchWatchlist(userId: string): Promise<WatchlistItem[]> {
  const res = await fetch(`${API_URL_ROOT}/profiles/${userId}/watchlist`);
  if (!res.ok) throw new Error("Failed to load watchlist");
  return res.json();
}
