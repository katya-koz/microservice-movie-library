import { API_URL_ROOT } from "@/lib/config";
import { uploadWithProgress } from "@/lib/uploadClient";
import type {
  AcceptedFileResponse,
  CreateJobResponse,
  FileStatusResponse,
  UploadJobInfo,
} from "@/types/uploadPipeline";

/** Thrown by waitForValidation when the caller cancels (reset / unmount). */
export class CancelledError extends Error {
  constructor() {
    super("Cancelled");
    this.name = "CancelledError";
  }
}

/** Upload-service errors are `{ message }`; fall back to a status line. */
async function errorMessage(res: Response, fallback: string): Promise<string> {
  try {
    const body = (await res.json()) as { message?: string } | null;
    if (body?.message) return body.message;
  } catch {
    // empty / non-JSON body (e.g. a bare 404)
  }
  return `${fallback} (HTTP ${res.status})`;
}

/**
 * POST /uploads/upload-job
 * The controller takes @RequestParam values (not a JSON body), so this is sent
 * form-encoded: tmdb_id, media_type, userId.
 */
export async function createUploadJob(args: {
  tmdbId: number;
  mediaType: "MOVIE" | "SHOW";
  userId: string;
}): Promise<string> {
  const body = new URLSearchParams({
    tmdb_id: String(args.tmdbId),
    media_type: args.mediaType,
    userId: args.userId,
  });

  const res = await fetch(`${API_URL_ROOT}/uploads/upload-job`, {
    method: "POST",
    body,
  });
  if (!res.ok) {
    throw new Error(await errorMessage(res, "Could not create upload job"));
  }

  const data = (await res.json()) as CreateJobResponse;
  return data.job_id;
}

/**
 * GET /uploads/upload-job/{jobId}?userId=
 * 404 covers both "no such job" and "not your job".
 */
export async function fetchUploadJobInfo(
  jobId: string,
  userId: string,
): Promise<UploadJobInfo> {
  const params = new URLSearchParams({ userId });
  const res = await fetch(
    `${API_URL_ROOT}/uploads/upload-job/${jobId}?${params.toString()}`,
  );
  if (res.status === 404) throw new Error("Upload job not found");
  if (!res.ok) {
    throw new Error(await errorMessage(res, "Failed to load upload job"));
  }
  return res.json();
}

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

/**
 * Polls the job until catalog validation finishes. The PUT endpoint answers 409
 * until validation_status is COMPLETED, so uploads must wait for this.
 */
export async function waitForValidation(
  jobId: string,
  userId: string,
  isCancelled: () => boolean,
  { intervalMs = 1000, timeoutMs = 120_000 } = {},
): Promise<UploadJobInfo> {
  const deadline = Date.now() + timeoutMs;

  while (true) {
    if (isCancelled()) throw new CancelledError();

    const job = await fetchUploadJobInfo(jobId, userId);

    if (job.validation_status === "COMPLETED") return job;
    if (job.validation_status === "FAILED") {
      throw new Error(job.validation_message || "Catalog validation failed");
    }

    if (Date.now() > deadline) {
      throw new Error("Timed out waiting for catalog validation");
    }
    await sleep(intervalMs);
  }
}

/**
 * PUT /uploads/upload/{jobId}  (multipart)
 * Resolves with the 202 body once the file is stored and queued; progress after
 * that comes from GET /uploads/files/{id} or the status websocket.
 *
 * `tmdbId` is the movie's id, or the episode's id for shows (it must belong to
 * the job or the server answers 400). `media_type` is no longer needed by the
 * server, so it isn't sent.
 */
export async function uploadFile(args: {
  jobId: string;
  userId: string;
  tmdbId: number;
  language?: string;
  file: File;
  onProgress: (percent: number) => void;
  /** accepted for call-site compatibility; the server ignores media_type now */
  mediaType?: "MOVIE" | "SHOW";
}): Promise<AcceptedFileResponse> {
  const form = new FormData();
  form.append("tmdb_id", String(args.tmdbId));
  form.append("userId", args.userId);
  if (args.language) form.append("language", args.language);
  form.append("file", args.file);

  const { body } = await uploadWithProgress(
    `${API_URL_ROOT}/uploads/upload/${args.jobId}`,
    form,
    args.onProgress,
    "PUT",
  );
  return body as AcceptedFileResponse;
}

/** GET /uploads/files/{id}?userId=  (same shape the websocket pushes, minus `message`) */
export async function fetchFileStatus(
  fileUploadId: string,
  userId: string,
): Promise<FileStatusResponse> {
  const params = new URLSearchParams({ userId });
  const res = await fetch(
    `${API_URL_ROOT}/uploads/files/${fileUploadId}?${params.toString()}`,
  );
  if (!res.ok) {
    throw new Error(await errorMessage(res, "Failed to load file status"));
  }
  return res.json();
}
