import {
  STATUS_ORDER,
  UploadJobsPage,
  UploadJobStatus,
  UploadStatus,
} from "@/types/uploadJobs";
import { UPLOAD_API_URL } from "./config";

export function statusProgress(status: UploadStatus): number {
  if (status === "FAILURE") return 0;

  const index = STATUS_ORDER.indexOf(status);
  if (index === -1) return 0;

  return Math.round((index / (STATUS_ORDER.length - 1)) * 100);
}

export function isTerminalStatus(status: UploadStatus): boolean {
  return status === "COMPLETED" || status === "FAILURE";
}
