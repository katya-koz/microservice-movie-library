import { Suspense } from "react";
import WatchClient from "../WatchClient";

export default function WatchPage() {
  return (
    <Suspense
      fallback={
        <div className="fixed inset-0 flex items-center justify-center bg-black">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-slate-700 border-t-white" />
        </div>
      }
    >
      <WatchClient />
    </Suspense>
  );
}
