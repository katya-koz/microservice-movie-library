"use client";

import {
  ReactNode,
  createContext,
  useContext,
  useEffect,
  useState,
} from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Search } from "react-bootstrap-icons";

type SearchContextValue = {
  /** The committed search term currently in the URL (?title=...). */
  query: string;
  /** Ready-to-render search input. */
  searchBar: ReactNode;
};

const SearchContext = createContext<SearchContextValue | null>(null);

/** Returns null when there is no <SearchFeature> above in the tree. */
export function useSearchFeature() {
  return useContext(SearchContext);
}

function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const handle = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(handle);
  }, [value, delayMs]);

  return debounced;
}

type SearchFeatureProps = {
  children: ReactNode;
  placeholder?: string;
  /** URL param used for the search term. */
  param?: string;
  debounceMs?: number;
};

export default function SearchFeature({
  children,
  placeholder = "Search",
  param = "title",
  debounceMs = 400,
}: SearchFeatureProps) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const urlSearch = searchParams.get(param) ?? "";

  const [searchInput, setSearchInput] = useState(urlSearch);
  const debouncedSearch = useDebouncedValue(searchInput, debounceMs);

  // URL -> input (back/forward buttons, external navigation).
  useEffect(() => {
    setSearchInput(urlSearch);
  }, [urlSearch]);

  // Debounced input -> URL.
  // Deliberately depends only on debouncedSearch: adding urlSearch/searchParams
  // would re-run this when the URL changes and push the stale value back.
  useEffect(() => {
    const trimmed = debouncedSearch.trim();

    if (trimmed === urlSearch) {
      return;
    }

    const params = new URLSearchParams(searchParams.toString());

    if (trimmed) {
      params.set(param, trimmed);
    } else {
      params.delete(param);
    }

    params.set("page", "1");

    router.push(`${pathname}?${params.toString()}`);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedSearch]);

  const searchBar = (
    <div className="relative w-full max-w-sm">
      <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />

      <input
        type="text"
        value={searchInput}
        onChange={(e) => setSearchInput(e.target.value)}
        placeholder={placeholder}
        aria-label={placeholder}
        className="w-full rounded-md border border-slate-700 bg-slate-900 py-2 pl-9 pr-9 text-sm text-slate-100 placeholder-slate-500 outline-none transition focus:border-slate-500"
      />

      {searchInput && (
        <button
          type="button"
          onClick={() => setSearchInput("")}
          aria-label="Clear search"
          className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-500 transition hover:text-slate-300"
        >
          <svg
            className="h-4 w-4"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M6 18L18 6M6 6l12 12"
            />
          </svg>
        </button>
      )}
    </div>
  );

  return (
    <SearchContext.Provider value={{ query: urlSearch, searchBar }}>
      {children}
    </SearchContext.Provider>
  );
}
