export interface UploadResponse {
  status: number;
  body: unknown;
}

export function uploadWithProgress(
  url: string,
  formData: FormData,
  onProgress: (percent: number) => void
): Promise<UploadResponse> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', url);

    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) onProgress(Math.round((e.loaded / e.total) * 100));
    };

    xhr.onload = () => {
      let body: unknown = null;
      try {
        body = xhr.responseText ? JSON.parse(xhr.responseText) : null;
      } catch {
        body = xhr.responseText;
      }
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve({ status: xhr.status, body });
      } else {
        const message = (body as { message?: string } | null)?.message ?? `Upload failed (HTTP ${xhr.status})`;
        reject(new Error(message));
      }
    };

    xhr.onerror = () => reject(new Error('Network error during upload — is the upload service reachable?'));
    xhr.send(formData);
  });
}
