"use client";

import { ReactNode } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useSearchFeature } from "@/components/media/SearchFeature";

type PaginatedMediaPageProps<T> = {
  title: string;
  media: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  loading: boolean;
  error: Error | null;
  renderItem: (item: T) => ReactNode;
};

export default function PaginatedMediaPage<T>({
  title,
  media,
  totalElements,
  totalPages,
  currentPage,
  loading,
  error,
  renderItem,
}: PaginatedMediaPageProps<T>) {
  const router = useRouter();
  const pathname = usePathname();

  // Optional: only present when wrapped in <SearchFeature>.
  const searchFeature = useSearchFeature();

  const changePage = (page: number) => {
    // Read at click time (not render time), so no useSearchParams / Suspense needed.
    const params = new URLSearchParams(window.location.search);
    params.set("page", page.toString());
    router.push(`${pathname}?${params.toString()}`);
  };

  const showContent = !loading && !error;

  return (
    <main className="mx-auto max-w-7xl px-6 py-10">
      {/* Header (same structure in every state so the search input keeps focus) */}
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-4xl font-bold">{title}</h1>

        <div className="flex items-center gap-4">
          {searchFeature?.searchBar}

          {showContent && (
            <span className="whitespace-nowrap text-sm text-slate-500">
              {totalElements} {totalElements === 1 ? "item" : "items"}
            </span>
          )}
        </div>
      </div>

      {loading ? (
        <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 md:grid-cols-4">
          {Array.from({ length: 24 }).map((_, index) => (
            <div
              key={index}
              className="aspect-[2/3] animate-pulse rounded-lg bg-slate-800"
            />
          ))}
        </div>
      ) : error ? (
        <>
          <p className="mt-4 text-red-400">
            Failed to load {title.toLowerCase()}.
          </p>
          <p className="mt-2 text-sm text-slate-500">{error.message}</p>
        </>
      ) : media.length > 0 ? (
        <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 md:grid-cols-4">
          {media.map(renderItem)}
        </div>
      ) : (
        <div className="flex min-h-64 items-center justify-center rounded-lg border border-dashed border-slate-800">
          <p className="text-slate-500">
            {searchFeature?.query
              ? `No ${title.toLowerCase()} found for "${searchFeature.query}".`
              : `No ${title.toLowerCase()} found.`}
          </p>
        </div>
      )}

      {showContent && totalPages > 1 && (
        <div className="mt-10 flex items-center justify-center gap-4">
          <button
            disabled={currentPage === 1}
            onClick={() => changePage(currentPage - 1)}
            className="rounded-md bg-slate-800 px-4 py-2 text-sm transition hover:bg-slate-700 disabled:opacity-40"
          >
            Previous
          </button>

          <span className="text-sm text-slate-400">
            {currentPage} / {totalPages}
          </span>

          <button
            disabled={currentPage === totalPages}
            onClick={() => changePage(currentPage + 1)}
            className="rounded-md bg-slate-800 px-4 py-2 text-sm transition hover:bg-slate-700 disabled:opacity-40"
          >
            Next
          </button>
        </div>
      )}
    </main>
  );
}
