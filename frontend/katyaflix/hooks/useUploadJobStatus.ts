"use client";

import { useEffect, useRef, useState } from "react";

import { UploadJobStatus } from "@/types/uploadJobs";
import { WS_URL_ROOT } from "@/lib/config";
import { fetchUploadJob } from "@/api/upload";
import { isTerminalStatus } from "@/lib/uploadJobs";

interface UseUploadJobStatusResult {
  job: UploadJobStatus | null;
  connected: boolean;
  error: string | null;
  isTerminal: boolean;
}

/**
 * Opens a websocket to /ws/upload-jobs/{jobId} and keeps `job` in sync with
 * whatever the server pushes (see UploadJobWebSocketHandler /
 * UploadJobChangeStreamWatcher on the backend). Falls back to a single REST
 * fetch for the very first paint so there's something to show before the
 * socket handshake completes.
 */
export function useUploadJobStatus(
  jobId: string | null,
): UseUploadJobStatusResult {
  const [job, setJob] = useState<UploadJobStatus | null>(null);
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const socketRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    if (!jobId) {
      setJob(null);
      return;
    }

    let cancelled = false;
    setError(null);

    fetchUploadJob(jobId)
      .then((initial) => {
        if (!cancelled) setJob(initial);
      })
      .catch(() => {
        // not fatal - the socket's initial snapshot will populate this
      });

    const socket = new WebSocket(`${WS_URL_ROOT}/upload-jobs/${jobId}`);
    socketRef.current = socket;

    socket.onopen = () => setConnected(true);

    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as UploadJobStatus;
        setJob(data);
      } catch {
        // ignore malformed payloads rather than crashing the view
      }
    };

    socket.onerror = () =>
      setError("Lost connection to the job status stream.");

    socket.onclose = () => setConnected(false);

    return () => {
      cancelled = true;
      socket.close();
      socketRef.current = null;
    };
  }, [jobId]);

  return {
    job,
    connected,
    error,
    isTerminal: job ? isTerminalStatus(job.status) : false,
  };
}
