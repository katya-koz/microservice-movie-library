export type FileKind = "VIDEO" | "SUBTITLE";

/** Mirrors the backend FileUploadStatus enum. */
export type ServerFileStatus =
  | "UPLOADING"
  | "ENQUEUED"
  | "PENDING"
  | "ENCODING"
  | "DOWNLOADING_ASSETS"
  | "FINALIZING"
  | "AWAITING_ENRICHMENT"
  | "COMPLETED"
  | "FAILED";

// ---------------------------------------------------------------------------
// API shapes (snake_case = exactly what the Spring controller returns)
// ---------------------------------------------------------------------------

export interface CreateJobResponse {
  job_id: string;
}

export interface JobEpisodeRef {
  season_number: number;
  episode_number: number;
  tmdb_id: number;
}

export interface UploadJobInfo {
  id: string;
  media_type: "MOVIE" | "SHOW";
  title: string;
  tmdb_id: number;
  /** backend PipelineStatus name: IN_PROGRESS | COMPLETED | FAILED */
  validation_status: string;
  validation_message: string | null;
  /** empty for movies */
  episodes: JobEpisodeRef[];
}

export interface AcceptedFileResponse {
  file_upload_id: string;
  status: ServerFileStatus;
}

/** Same shape for GET /uploads/files/{id} and the websocket pushes (FileStatusDto). */
export interface FileStatusResponse {
  file_upload_id: string;
  upload_job_id: string;
  status: ServerFileStatus;
  progress_percent: number;
  attempts: number;
  error: string | null;
  /**
   * current human-readable detail from the pipeline, e.g. "Encoding 42%".
   * Only present on websocket pushes; GET /uploads/files/{id} doesn't return it.
   */
  message?: string | null;
  final_path: string | null;
}

/** Messages pushed by /ws/upload-pipeline/{jobId} */
export type PipelineSocketMessage =
  | { type: "snapshot"; files: FileStatusResponse[] }
  | { type: "file"; file: FileStatusResponse };

// ---------------------------------------------------------------------------
// Planning: one entry per HTTP upload we are going to make
// ---------------------------------------------------------------------------

export interface PlannedFile {
  key: string;
  filename: string;
  fileType: FileKind;
  /** human label, e.g. "S01EP02", "S01EP02 subtitle (en)", "Movie" */
  label: string;
  seasonNumber?: number;
  episodeNumber?: number;
  language?: string;
  file: File;
}

// ---------------------------------------------------------------------------
// UI state
// ---------------------------------------------------------------------------

/**
 * waiting        - not started yet
 * uploading      - HTTP transfer in progress (progress bar)
 * server         - transfer finished; follow `serverStatus` from the websocket
 * upload-failed  - the HTTP call itself failed / was rejected
 */
export type FileStage = "waiting" | "uploading" | "server" | "upload-failed";

export interface PipelineFile {
  key: string;
  label: string;
  filename: string;
  fileType: FileKind;
  stage: FileStage;
  uploadPercent: number;
  fileUploadId: string | null;
  serverStatus: ServerFileStatus | null;
  encodePercent: number;
  attempts: number;
  error: string | null;
  /** latest detail line from the server pipeline */
  message: string | null;
  /** upload-failed files that are worth trying again (not "episode doesn't exist on TMDB") */
  retryable: boolean;
}

export type PipelinePhase =
  | "idle"
  | "creating-job"
  | "validating"
  | "transferring"
  | "finished"
  | "failed";

export interface PipelineState {
  phase: PipelinePhase;
  jobId: string | null;
  jobTitle: string | null;
  /** set when the whole pipeline failed (job creation / catalog validation) */
  error: string | null;
  files: PipelineFile[];
}

export const initialPipelineState: PipelineState = {
  phase: "idle",
  jobId: null,
  jobTitle: null,
  error: null,
  files: [],
};
