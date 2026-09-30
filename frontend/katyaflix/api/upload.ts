import { API_URL_ROOT } from "@/lib/config";
import { UploadJobsPage, UploadJobStatus } from "@/types/uploadJobs";

export async function fetchUploadJob(jobId: string): Promise<UploadJobStatus> {
  const res = await fetch(`${API_URL_ROOT}/uploads/jobs/${jobId}`);

  if (!res.ok) {
    throw new Error(`Failed to load job ${jobId}`);
  }

  return res.json();
}

export async function fetchUploadJobs(
  page = 0,
  size = 20,
  userId: string,
): Promise<UploadJobsPage> {
  const res = await fetch(
    `${API_URL_ROOT}/uploads/jobs?page=${page}&size=${size}&userId=${userId}`,
  );

  if (!res.ok) {
    throw new Error("Failed to load upload jobs");
  }

  return res.json();
}
