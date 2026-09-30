import { useMutation } from "@tanstack/react-query";
import { uploadWithProgress } from "@/lib/uploadClient";
import { API_URL_ROOT } from "@/lib/config";
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
        `${API_URL_ROOT}/uploads`,
        formData,
        onProgress,
      );

      return {
        jobId: (body as { jobId?: string } | null)?.jobId ?? "unknown",
      };
    },
  });
}
