"use client";

import { useEffect, useRef, useState } from "react";
import { searchTmdb, tmdbImage, TmdbMediaType } from "@/lib/tmdb";
import { TmdbSearchResult } from "@/lib/types";

interface TmdbSearchProps {
  type: TmdbMediaType;
  selected: TmdbSearchResult | null;
  onSelect: (result: TmdbSearchResult | null) => void;
}

export default function TmdbSearch({
  type,
  selected,
  onSelect,
}: TmdbSearchProps) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<TmdbSearchResult[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function onClickOutside(e: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(e.target as Node)
      ) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", onClickOutside);
    return () => document.removeEventListener("mousedown", onClickOutside);
  }, []);

  // useEffect(() => {
  //   if (!query.trim()) {
  //     setResults([]);
  //     setError(null);
  //     return;
  //   }
  //   setIsLoading(true);
  //   setError(null);
  //   const timeout = setTimeout(async () => {
  //     try {
  //       const res = await searchTmdb(type, query);
  //       setResults(res);
  //       setIsOpen(true);
  //     } catch (err) {
  //       setError(err instanceof Error ? err.message : 'Search failed');
  //     } finally {
  //       setIsLoading(false);
  //     }
  //   }, 350);
  //   return () => clearTimeout(timeout);
  // }, [query, type]);

  useEffect(() => {
    if (!query.trim()) {
      setResults([]);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);

    const controller = new AbortController();

    // debounce and abort search on change
    const timeout = setTimeout(async () => {
      try {
        const res = await searchTmdb(type, query, controller.signal);

        // Only update state if this request wasn't cancelled
        if (!controller.signal.aborted) {
          setResults(res);
          setIsOpen(true);
        }
      } catch (err) {
        // AbortError is expected when the user types again
        if (err instanceof DOMException && err.name === "AbortError") {
          return;
        }

        if (!controller.signal.aborted) {
          setError(err instanceof Error ? err.message : "Search failed");
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    }, 350);

    return () => {
      clearTimeout(timeout);
      controller.abort();
    };
  }, [query, type]);

  if (selected) {
    return (
      <div className="flex items-center gap-4 rounded-lg border border-ink-line bg-ink-raised p-4">
        <div className="h-28 w-20 flex-shrink-0 overflow-hidden rounded bg-ink-elevated">
          {tmdbImage(selected.posterPath) ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={tmdbImage(selected.posterPath, "w200")!}
              alt=""
              className="h-full w-full object-cover"
            />
          ) : (
            <PosterFallback />
          )}
        </div>
        <div className="min-w-0 flex-1">
          <p className="eyebrow">Selected {type === "tv" ? "show" : "movie"}</p>
          <p className="mt-1 truncate font-display text-xl font-bold text-paper">
            {selected.title}
          </p>
          <p className="font-mono text-sm text-paper-muted">{selected.year}</p>
        </div>
        <button
          type="button"
          onClick={() => {
            onSelect(null);
            setQuery("");
          }}
          className="flex-shrink-0 rounded border border-ink-line px-3 py-1.5 font-mono text-xs uppercase tracking-widest text-paper-muted hover:border-paper-faint hover:text-paper"
        >
          Change
        </button>
      </div>
    );
  }

  return (
    <div ref={containerRef} className="relative">
      <input
        type="text"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onFocus={() => results.length > 0 && setIsOpen(true)}
        placeholder={
          type === "tv"
            ? "Search for a show on TMDB…"
            : "Search for a movie on TMDB…"
        }
        className="w-full rounded-lg border border-ink-line bg-ink-raised px-4 py-3 text-paper placeholder:text-paper-faint focus:border-marquee"
      />

      {isOpen && query.trim().length > 0 && (
        <div className="absolute z-20 mt-2 max-h-96 w-full overflow-y-auto rounded-lg border border-ink-line bg-ink-raised shadow-modal animate-rise-in">
          {isLoading && (
            <p className="p-4 font-mono text-xs text-paper-muted">Searching…</p>
          )}

          {!isLoading && error && (
            <p className="p-4 font-mono text-xs text-signal-error">{error}</p>
          )}

          {!isLoading && !error && results.length === 0 && (
            <p className="p-4 font-mono text-xs text-paper-muted">
              No results for &ldquo;{query}&rdquo;.
            </p>
          )}

          {!isLoading &&
            !error &&
            results.map((result) => (
              <button
                key={result.id}
                type="button"
                onClick={() => {
                  onSelect(result);
                  setIsOpen(false);
                }}
                className="flex w-full items-center gap-3 border-b border-ink-line/60 p-3 text-left last:border-b-0 hover:bg-ink-elevated"
              >
                <div className="h-16 w-11 flex-shrink-0 overflow-hidden rounded bg-ink-elevated">
                  {tmdbImage(result.posterPath) ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                      src={tmdbImage(result.posterPath, "w200")!}
                      alt=""
                      className="h-full w-full object-cover"
                    />
                  ) : (
                    <PosterFallback small />
                  )}
                </div>
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-paper">
                    {result.title}
                  </p>
                  <p className="font-mono text-xs text-paper-muted">
                    {result.year}
                  </p>
                </div>
              </button>
            ))}
        </div>
      )}
    </div>
  );
}

function PosterFallback({ small }: { small?: boolean }) {
  return (
    <div className="flex h-full w-full items-center justify-center text-paper-faint">
      <svg
        viewBox="0 0 24 24"
        fill="none"
        className={small ? "h-4 w-4" : "h-6 w-6"}
        aria-hidden="true"
      >
        <rect
          x="3"
          y="3"
          width="18"
          height="18"
          rx="2"
          stroke="currentColor"
          strokeWidth="1.5"
        />
        <path
          d="M3 16l5-5 4 4 3-3 6 6"
          stroke="currentColor"
          strokeWidth="1.5"
        />
        <circle cx="8" cy="8" r="1.5" fill="currentColor" />
      </svg>
    </div>
  );
}
