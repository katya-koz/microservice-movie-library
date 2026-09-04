"use client";

import { ReactNode } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

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
  const searchParams = useSearchParams();

  const changePage = (page: number) => {
    const params = new URLSearchParams(searchParams.toString());

    params.set("page", page.toString());

    router.push(`${pathname}?${params.toString()}`);
  };

  if (loading) {
    return (
      <main className="mx-auto max-w-7xl px-6 py-8">
        <h1 className="mb-6 text-2xl font-bold">{title}</h1>

        <div className="grid grid-cols-2 gap-5 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
          {Array.from({ length: 24 }).map((_, index) => (
            <div
              key={index}
              className="aspect-[2/3] animate-pulse rounded-lg bg-slate-800"
            />
          ))}
        </div>
      </main>
    );
  }

  if (error) {
    return (
      <main className="mx-auto max-w-7xl px-6 py-8">
        <h1 className="text-2xl font-bold">{title}</h1>

        <p className="mt-4 text-red-400">
          Failed to load {title.toLowerCase()}.
        </p>

        <p className="mt-2 text-sm text-slate-500">{error.message}</p>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-7xl px-6 py-8">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold">{title}</h1>

        <span className="text-sm text-slate-500">
          {totalElements} {totalElements === 1 ? "item" : "items"}
        </span>
      </div>

      {/* Grid */}
      {media.length > 0 ? (
        <div className="grid grid-cols-2 gap-5 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
          {media.map(renderItem)}
        </div>
      ) : (
        <div className="flex min-h-64 items-center justify-center rounded-lg border border-dashed border-slate-800">
          <p className="text-slate-500">No {title.toLowerCase()} found.</p>
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
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
