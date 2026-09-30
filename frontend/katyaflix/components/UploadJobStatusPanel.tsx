"use client";

import { useUploadJobStatus } from "@/hooks/useUploadJobStatus";
import { STATUS_LABELS } from "@/types/uploadJobs";

export default function UploadJobStatusPanel({ jobId }: { jobId: string }) {
  const { job, connected, error } = useUploadJobStatus(jobId);

  if (!job) {
    return (
      <div className="rounded-lg border border-ink-line bg-ink-raised p-4">
        <p className="font-mono text-xs text-paper-muted">
          Connecting to job {jobId}…
        </p>
      </div>
    );
  }

  const failed = job.status === "FAILURE";

  return (
    <div className="rounded-lg border border-ink-line bg-ink-raised p-4">
      <div className="flex items-center justify-between">
        <p className="font-mono text-xs uppercase tracking-widest text-paper-muted">
          {job.title}
        </p>

        <span
          className={`font-mono text-[10px] uppercase tracking-widest ${
            connected ? "text-reel" : "text-paper-faint"
          }`}
        >
          {connected ? "live" : "reconnecting…"}
        </span>
      </div>

      <p
        className={`mt-1 font-mono text-sm ${
          failed ? "text-signal-error" : "text-paper"
        }`}
      >
        {STATUS_LABELS[job.status]}
        {job.currentStep ? ` — ${job.currentStep}` : ""}
      </p>

      {!failed && (
        <div className="mt-3 h-1.5 w-full overflow-hidden rounded-full bg-ink-line">
          <div className="h-full rounded-full bg-marquee transition-all" />
        </div>
      )}

      {job.errorMessages.length > 0 && (
        <ul className="mt-3 space-y-1">
          {job.errorMessages.map((message, i) => (
            <li key={i} className="font-mono text-xs text-signal-error">
              {message}
            </li>
          ))}
        </ul>
      )}

      {error && (
        <p className="mt-3 font-mono text-xs text-paper-muted">{error}</p>
      )}
    </div>
  );
}
