"use client";

import { useState } from "react";
import Link from "next/link";

import { useUploadJobs } from "@/hooks/useUploadJobs";
import UploadJobStatusPanel from "@/components/UploadJobStatusPanel";
import { STATUS_LABELS, UploadJobStatus } from "@/types/uploadJobs";
import { useUser } from "@/context/UserContext";

function StatusBadge({ status }: { status: UploadJobStatus["status"] }) {
  const tone =
    status === "FAILURE"
      ? "text-signal-error border-signal-error/40"
      : status === "COMPLETED"
        ? "text-reel border-reel/40"
        : "text-paper-muted border-ink-line";

  return (
    <span
      className={`rounded-full border px-2 py-0.5 font-mono text-[10px] uppercase tracking-widest ${tone}`}
    >
      {STATUS_LABELS[status]}
    </span>
  );
}

export default function MyUploadJobsPage() {
  const user = useUser();
  const [page, setPage] = useState(0);
  const [selectedJobId, setSelectedJobId] = useState<string | null>(null);

  const { data, isLoading, error } = useUploadJobs(page, 20, user.user?.id);

  return (
    <div className="mx-auto max-w-6xl px-6 py-12">
      <p className="eyebrow">Add to library</p>

      <div className="mt-2 flex items-center justify-between">
        <h1 className="font-display text-4xl font-black uppercase tracking-tightest text-paper">
          My Upload Jobs
        </h1>

        <Link
          href="/upload"
          className="font-mono text-xs uppercase tracking-widest text-paper-muted hover:text-paper"
        >
          ← Back to upload
        </Link>
      </div>

      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_360px]">
        <div className="rounded-lg border border-ink-line bg-ink-raised">
          {isLoading && (
            <p className="p-4 font-mono text-xs text-paper-muted">
              Loading jobs…
            </p>
          )}

          {error && (
            <p className="p-4 font-mono text-xs text-signal-error">
              Failed to load upload jobs.
            </p>
          )}

          {data && data.content.length === 0 && (
            <p className="p-4 font-mono text-xs text-paper-muted">
              No uploads yet.
            </p>
          )}

          {data && data.content.length > 0 && (
            <table className="w-full text-left">
              <thead>
                <tr className="border-b border-ink-line font-mono text-[10px] uppercase tracking-widest text-paper-faint">
                  <th className="px-4 py-2">Title</th>
                  <th className="px-4 py-2">Type</th>
                  <th className="px-4 py-2">Status</th>
                  <th className="px-4 py-2">Updated</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((job) => (
                  <tr
                    key={job.id}
                    onClick={() => setSelectedJobId(job.id)}
                    className={`cursor-pointer border-b border-ink-line/60 hover:bg-ink-line/20 ${
                      selectedJobId === job.id ? "bg-ink-line/30" : ""
                    }`}
                  >
                    <td className="px-4 py-2 font-mono text-sm text-paper">
                      {job.title}
                    </td>
                    <td className="px-4 py-2 font-mono text-xs uppercase text-paper-muted">
                      {job.mediaType}
                    </td>
                    <td className="px-4 py-2">
                      <StatusBadge status={job.status} />
                    </td>
                    <td className="px-4 py-2 font-mono text-xs text-paper-muted">
                      {new Date(job.updatedAt).toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {data && data.totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-ink-line px-4 py-2 font-mono text-xs text-paper-muted">
              <button
                type="button"
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="uppercase tracking-widest disabled:opacity-30"
              >
                ← Prev
              </button>
              <span>
                Page {page + 1} of {data.totalPages}
              </span>
              <button
                type="button"
                disabled={page + 1 >= data.totalPages}
                onClick={() => setPage((p) => p + 1)}
                className="uppercase tracking-widest disabled:opacity-30"
              >
                Next →
              </button>
            </div>
          )}
        </div>

        <div>
          {selectedJobId ? (
            <UploadJobStatusPanel jobId={selectedJobId} />
          ) : (
            <div className="rounded-lg border border-ink-line bg-ink-raised p-4">
              <p className="font-mono text-xs text-paper-muted">
                Select a job to see its live status.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
