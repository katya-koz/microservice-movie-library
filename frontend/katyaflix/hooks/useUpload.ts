import { useMutation } from "@tanstack/react-query";
import { uploadWithProgress } from "@/lib/uploadClient";

const UPLOAD_API_URL =
  process.env.NEXT_PUBLIC_API_ROOT ?? "http://localhost:8080";

export function useUpload() {
  return useMutation({
    mutationFn: async ({
      formData,
      onProgress,
    }: {
      formData: FormData;
      onProgress: (progress: number) => void;
    }) => {
      const { body } = await uploadWithProgress(
        `${UPLOAD_API_URL}/uploads`,
        formData,
        onProgress,
      );

      return {
        jobId: (body as { jobId?: string } | null)?.jobId ?? "unknown",
      };
    },
  });
}
