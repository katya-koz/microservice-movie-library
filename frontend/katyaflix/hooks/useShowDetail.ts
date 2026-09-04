// "use client";

// import { useQuery } from "@tanstack/react-query";
// import { getShowDetail } from "@/api/shows";

// export function useShowDetail(id: string) {
//   return useQuery({
//     queryKey: ["show", id],
//     queryFn: () => getShowDetail(id),
//     enabled: !!id,
//   });
// }

"use client";

import { useEffect, useState } from "react";
import type { ShowDetail } from "@/types/show";

const API_URL_ROOT =
  process.env.NEXT_PUBLIC_API_URL_ROOT ?? "http://localhost:8080/api";

type UseShowDetailResult = {
  data: ShowDetail | undefined;
  isLoading: boolean;
  error: Error | null;
};

// GET /api/shows/{id}
export function useShowDetail(showId: string | null): UseShowDetailResult {
  const [data, setData] = useState<ShowDetail | undefined>(undefined);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    if (!showId) {
      setData(undefined);
      setIsLoading(false);
      setError(null);
      return;
    }

    const controller = new AbortController();

    setIsLoading(true);
    setError(null);

    fetch(`${API_URL_ROOT}/shows/${showId}`, { signal: controller.signal })
      .then(async (res) => {
        if (!res.ok) {
          throw new Error(`Failed to load show (${res.status})`);
        }
        return (await res.json()) as ShowDetail;
      })
      .then((show) => setData(show))
      .catch((err: unknown) => {
        if (err instanceof Error && err.name !== "AbortError") {
          setError(err);
        }
      })
      .finally(() => setIsLoading(false));

    return () => controller.abort();
  }, [showId]);

  return { data, isLoading, error };
}
