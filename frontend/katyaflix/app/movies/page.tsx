import { Suspense } from "react";
import MoviesPageClient from "@/components/movies/MoviesPageClient";

export default function MoviesPage() {
  return (
    <Suspense fallback={null}>
      <MoviesPageClient />
    </Suspense>
  );
}
