import { useMutation } from "@tanstack/react-query";
import { uploadWithProgress } from "@/lib/uploadClient";

const UPLOAD_API_URL =
  process.env.NEXT_PUBLIC_API_ROOT ?? "http://localhost:8080";
export function useUpload() {
  return useMutation({
    mutationFn: async ({
      formData,
      userId,
      onProgress,
    }: {
      formData: FormData;
      userId: string;
      onProgress: (progress: number) => void;
    }) => {
      formData.append("userId", userId);

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
