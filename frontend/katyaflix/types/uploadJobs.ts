export type UploadStatus =
  | "PENDING"
  | "SAVING_ASSETS"
  | "QUEUED_FOR_ENCODING"
  | "ENCODING"
  | "AWAITING_CATALOG"
  | "READY_TO_FINALIZE"
  | "FINALIZING"
  | "COMPLETED"
  | "FAILURE";

export interface UploadJobStatus {
  id: string;
  mediaType: string;
  title: string;
  status: UploadStatus;
  currentStep: string | null;
  tmdbId: number;
  catalogValidationStatus: boolean;
  fileUploadStatus: boolean;
  mediaFileEnrichmentStatus: boolean;
  filePathUpdateStatus: boolean;
  encodedFileCount: number;
  errorMessages: string[];
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
}

export interface UploadJobsPage {
  content: UploadJobStatus[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export const STATUS_ORDER: UploadStatus[] = [
  "PENDING",
  "SAVING_ASSETS",
  "QUEUED_FOR_ENCODING",
  "ENCODING",
  "AWAITING_CATALOG",
  "FINALIZING",
  "COMPLETED",
];

export const STATUS_LABELS: Record<UploadStatus, string> = {
  PENDING: "Queued",
  SAVING_ASSETS: "Saving files",
  QUEUED_FOR_ENCODING: "Waiting for an encoder",
  ENCODING: "Encoding",
  AWAITING_CATALOG: "Waiting on catalog",
  READY_TO_FINALIZE: "Ready to finalize",
  FINALIZING: "Finalizing",
  COMPLETED: "Completed",
  FAILURE: "Failed",
};
