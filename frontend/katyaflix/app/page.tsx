"use client";

import MediaCarousel from "@/components/MediaCarousel";
import { useWatchlist } from "@/hooks/useWatchlist";
import { useCurrentlyWatching } from "@/hooks/useCurrentlyWatching";
import { useMediaSummaries } from "@/hooks/useCatalogSummaries";
import { useUser } from "@/context/UserContext";

export default function Home() {
  const { user } = useUser();

  const {
    data: watchlist,
    isLoading: watchlistLoading,
    error: watchlistError,
  } = useWatchlist(user?.id);

  const {
    data: currentlyWatching,
    isLoading: currentlyWatchingLoading,
    error: currentlyWatchingError,
  } = useCurrentlyWatching(user?.id);

  const {
    data: myList,
    isLoading: myListLoading,
    error: myListError,
  } = useMediaSummaries(watchlist ?? []);

  const {
    data: currentlyWatchingMedia,
    isLoading: currentlyWatchingMediaLoading,
    error: currentlyWatchingMediaError,
  } = useMediaSummaries(currentlyWatching ?? []);

  const loading =
    watchlistLoading ||
    currentlyWatchingLoading ||
    myListLoading ||
    currentlyWatchingMediaLoading;

  const error =
    watchlistError ||
    currentlyWatchingError ||
    myListError ||
    currentlyWatchingMediaError;

  if (error) {
    return (
      <main className="mx-auto max-w-7xl px-6 py-10 text-white">
        <p className="text-red-400">Failed to load your media.</p>
      </main>
    );
  }

  return (
    <main className="min-h-screen mx-auto max-w-7xl px-6 py-10 flex flex-col gap-10">
      <h1 className="text-2xl italic">Hello, {user?.displayName}.</h1>

      <MediaCarousel
        title="Currently Watching"
        items={currentlyWatchingMedia}
        isLoading={loading}
      />

      <MediaCarousel title="My List" items={myList} isLoading={loading} />
    </main>
  );
}
