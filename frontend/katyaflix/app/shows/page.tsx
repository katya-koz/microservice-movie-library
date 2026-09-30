import { Suspense } from "react";
import ShowsPageClient from "@/components/shows/ShowsPageClient";

export default function ShowsPage() {
  return (
    <Suspense fallback={null}>
      <ShowsPageClient />
    </Suspense>
  );
}
