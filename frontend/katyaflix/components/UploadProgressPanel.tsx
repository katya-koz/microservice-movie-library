"use client";

import type { PipelineFile, PipelineState } from "@/types/uploadPipeline";

interface Props {
  state: PipelineState;
  /** true while the status websocket is connected */
  live?: boolean;
  canRetry?: boolean;
  onRetry?: () => void;
  onReset?: () => void;
}

// ---------------------------------------------------------------------------
// Job-level steps
// ---------------------------------------------------------------------------

type StepState = "done" | "active" | "pending" | "failed";

function jobSteps(state: PipelineState): { label: string; state: StepState }[] {
  const { phase, jobId } = state;
  const order = {
    "creating-job": 0,
    validating: 1,
    transferring: 2,
    finished: 3,
  } as const;
  const current = phase in order ? order[phase as keyof typeof order] : -1;

  const labels = [
    "Creating upload job",
    "Validating with catalog",
    "Uploading & processing files",
  ];

  return labels.map((label, i) => {
    if (phase === "failed") {
      // failed before a job existed -> step 0, otherwise it was catalog validation
      const failedAt = jobId ? 1 : 0;
      if (i < failedAt) return { label, state: "done" };
      if (i === failedAt) return { label, state: "failed" };
      return { label, state: "pending" };
    }
    if (current > i) return { label, state: "done" };
    if (current === i) return { label, state: "active" };
    return { label, state: "pending" };
  });
}

const stepIcon: Record<StepState, string> = {
  done: "✓",
  active: "…",
  pending: "○",
  failed: "✕",
};

const stepColor: Record<StepState, string> = {
  done: "text-green-600",
  active: "text-blue-600",
  pending: "text-gray-400",
  failed: "text-red-600",
};

// ---------------------------------------------------------------------------
// File-level status
// ---------------------------------------------------------------------------

function serverText(f: PipelineFile): { text: string; color: string } {
  const isSubtitle = f.fileType === "SUBTITLE";

  switch (f.serverStatus) {
    case "UPLOADING":
      return { text: "Saving upload", color: "text-gray-500" };
    case "PENDING":
      return { text: "Waiting to start", color: "text-gray-500" };
    case "DOWNLOADING_ASSETS":
      return { text: "Preparing file", color: "text-blue-600" };
    case "ENQUEUED":
      return f.attempts > 0
        ? {
            text: `Queued to retry (attempt ${f.attempts} failed)`,
            color: "text-amber-600",
          }
        : { text: "Queued for encoding", color: "text-gray-500" };
    case "ENCODING":
      return {
        text: isSubtitle
          ? "Converting subtitle"
          : `Encoding… ${f.encodePercent}%`,
        color: "text-blue-600",
      };
    case "FINALIZING":
      return { text: "Finishing up", color: "text-blue-600" };
    case "AWAITING_ENRICHMENT":
      return { text: "Adding to catalog", color: "text-blue-600" };
    case "COMPLETED":
      return { text: "Done", color: "text-green-600" };
    case "FAILED":
      return {
        text: `Failed${f.error ? `: ${f.error}` : ""}`,
        color: "text-red-600",
      };
    default:
      return { text: "Processing", color: "text-gray-500" };
  }
}

function Bar({ percent, label }: { percent: number; label: string }) {
  return (
    <div className="flex items-center gap-3">
      <div
        className="h-2 flex-1 overflow-hidden rounded bg-gray-200"
        role="progressbar"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={percent}
      >
        <div
          className="h-full bg-blue-600 transition-[width] duration-150"
          style={{ width: `${percent}%` }}
        />
      </div>
      <span className="w-28 text-right text-sm text-gray-600">{label}</span>
    </div>
  );
}

function FileRow({ file }: { file: PipelineFile }) {
  const server = serverText(file);
  const encoding =
    file.stage === "server" &&
    file.serverStatus === "ENCODING" &&
    file.fileType === "VIDEO";

  // The pipeline's own detail line ("Attempt 1 failed, re-queued: …"), shown only
  // when it adds something beyond the headline.
  const detail =
    file.message &&
    file.serverStatus !== "COMPLETED" &&
    !file.message.startsWith("Encoding ")
      ? file.message
      : null;

  return (
    <li className="flex flex-col gap-1 py-2">
      <div className="flex items-baseline justify-between gap-3">
        <span className="font-medium">{file.label}</span>
        <span className="truncate text-xs text-gray-500" title={file.filename}>
          {file.filename}
        </span>
      </div>

      {file.stage === "waiting" && (
        <span className="text-sm text-gray-500">Waiting to upload</span>
      )}

      {file.stage === "uploading" && (
        <Bar
          percent={file.uploadPercent}
          label={
            file.uploadPercent >= 100
              ? "Finishing upload…"
              : `Uploading ${file.uploadPercent}%`
          }
        />
      )}

      {file.stage === "upload-failed" && (
        <span className="text-sm text-red-600">
          Upload failed: {file.error}
        </span>
      )}

      {file.stage === "server" && (
        <>
          {encoding ? (
            <Bar percent={file.encodePercent} label={server.text} />
          ) : (
            <span className={`text-sm ${server.color}`}>{server.text}</span>
          )}
          {detail && <span className="text-xs text-gray-500">{detail}</span>}
        </>
      )}
    </li>
  );
}

// ---------------------------------------------------------------------------
// Panel
// ---------------------------------------------------------------------------

export function UploadProgressPanel({
  state,
  live,
  canRetry,
  onRetry,
  onReset,
}: Props) {
  if (state.phase === "idle") return null;

  const done = state.files.filter((f) => f.serverStatus === "COMPLETED").length;
  const failed = state.files.filter(
    (f) => f.serverStatus === "FAILED" || f.stage === "upload-failed",
  ).length;
  const uploaded = state.files.filter(
    (f) => f.stage === "server" || f.serverStatus,
  ).length;

  return (
    <div className="flex flex-col gap-4 rounded-lg border p-4">
      <div className="flex items-center justify-between gap-2">
        {state.jobTitle ? (
          <h3 className="text-lg font-semibold">{state.jobTitle}</h3>
        ) : (
          <span />
        )}
        {state.phase === "transferring" && (
          <span
            className={`text-xs ${live ? "text-green-600" : "text-amber-600"}`}
            aria-live="polite"
          >
            {live ? "● Live" : "○ Reconnecting…"}
          </span>
        )}
      </div>

      <ol className="flex flex-col gap-1">
        {jobSteps(state).map((step) => (
          <li
            key={step.label}
            className={`flex items-center gap-2 text-sm ${stepColor[step.state]}`}
          >
            <span className="w-4 text-center">{stepIcon[step.state]}</span>
            {step.label}
          </li>
        ))}
      </ol>

      {state.phase === "failed" && (
        <p className="text-sm text-red-600" role="alert">
          {state.error}
        </p>
      )}

      {state.phase === "transferring" && (
        <p className="text-sm text-gray-600" aria-live="polite">
          {uploaded} of {state.files.length} uploaded · {done} finished
          {failed > 0 ? ` · ${failed} failed` : ""}
        </p>
      )}

      {state.phase === "finished" && (
        <p
          className={`text-sm font-medium ${failed > 0 ? "text-amber-600" : "text-green-600"}`}
          aria-live="polite"
        >
          {failed === 0
            ? `All ${done} file${done === 1 ? "" : "s"} processed.`
            : `${done} done, ${failed} failed.`}
        </p>
      )}

      {state.files.length > 0 &&
        state.phase !== "creating-job" &&
        state.phase !== "validating" &&
        state.phase !== "failed" && (
          <ul className="divide-y">
            {state.files.map((f) => (
              <FileRow key={f.key} file={f} />
            ))}
          </ul>
        )}

      {(canRetry || state.phase === "finished" || state.phase === "failed") && (
        <div className="flex gap-2">
          {canRetry && onRetry && (
            <button
              type="button"
              onClick={onRetry}
              className="rounded border px-3 py-1 text-sm"
            >
              Retry failed uploads
            </button>
          )}
          {onReset && (
            <button
              type="button"
              onClick={onReset}
              className="rounded border px-3 py-1 text-sm"
            >
              {state.phase === "failed" ? "Start over" : "Done"}
            </button>
          )}
        </div>
      )}
    </div>
  );
}
