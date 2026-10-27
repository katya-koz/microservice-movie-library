"use client";

import { useCallback, useEffect, useReducer, useRef, useState } from "react";

import {
  CancelledError,
  createUploadJob,
  uploadFile,
  waitForValidation,
} from "@/api/uploadPipeline";
import { WS_URL_ROOT } from "@/lib/config";
import { BuiltLike, planFromBuilt } from "@/lib/uploadPlan";
import {
  FileStatusResponse,
  initialPipelineState,
  PipelineFile,
  PipelineSocketMessage,
  PipelineState,
  PlannedFile,
  ServerFileStatus,
} from "@/types/uploadPipeline";

export function isFileTerminal(f: PipelineFile): boolean {
  return (
    f.stage === "upload-failed" ||
    f.serverStatus === "COMPLETED" ||
    f.serverStatus === "FAILED"
  );
}

// ---------------------------------------------------------------------------
// Reducer
// ---------------------------------------------------------------------------

type Action =
  | { type: "start"; files: PlannedFile[] }
  | { type: "job-created"; jobId: string }
  | { type: "validated"; title: string }
  | { type: "fail"; error: string }
  | { type: "upload-started"; key: string }
  | { type: "upload-progress"; key: string; percent: number }
  | {
      type: "uploaded";
      key: string;
      fileUploadId: string;
      status: ServerFileStatus;
    }
  | { type: "upload-failed"; key: string; error: string; retryable: boolean }
  | { type: "server-status"; key: string; status: FileStatusResponse }
  | { type: "retry"; keys: string[] }
  | { type: "reset" };

function patch(
  state: PipelineState,
  key: string,
  changes: Partial<PipelineFile>,
): PipelineState {
  return {
    ...state,
    files: state.files.map((f) => (f.key === key ? { ...f, ...changes } : f)),
  };
}

/** Flip to "finished" once every file has reached a terminal state. */
function settle(state: PipelineState): PipelineState {
  if (
    state.phase === "transferring" &&
    state.files.length > 0 &&
    state.files.every(isFileTerminal)
  ) {
    return { ...state, phase: "finished" };
  }
  return state;
}

function reducer(state: PipelineState, action: Action): PipelineState {
  switch (action.type) {
    case "start":
      return {
        ...initialPipelineState,
        phase: "creating-job",
        files: action.files.map((f) => ({
          key: f.key,
          label: f.label,
          filename: f.filename,
          fileType: f.fileType,
          stage: "waiting",
          uploadPercent: 0,
          fileUploadId: null,
          serverStatus: null,
          encodePercent: 0,
          attempts: 0,
          error: null,
          message: null,
          retryable: false,
        })),
      };

    case "job-created":
      return { ...state, phase: "validating", jobId: action.jobId };

    case "validated":
      return { ...state, phase: "transferring", jobTitle: action.title };

    case "fail":
      return { ...state, phase: "failed", error: action.error };

    case "upload-started":
      return patch(state, action.key, {
        stage: "uploading",
        uploadPercent: 0,
        error: null,
      });

    case "upload-progress": {
      const f = state.files.find((x) => x.key === action.key);
      if (!f || (f.stage === "uploading" && f.uploadPercent === action.percent))
        return state;
      return patch(state, action.key, {
        stage: "uploading",
        uploadPercent: action.percent,
      });
    }

    case "uploaded":
      return patch(state, action.key, {
        stage: "server",
        uploadPercent: 100,
        fileUploadId: action.fileUploadId,
        serverStatus: action.status,
        error: null,
      });

    case "upload-failed":
      return settle(
        patch(state, action.key, {
          stage: "upload-failed",
          error: action.error,
          retryable: action.retryable,
        }),
      );

    case "server-status": {
      const s = action.status;
      return settle(
        patch(state, action.key, {
          stage: "server",
          serverStatus: s.status,
          encodePercent: s.progress_percent,
          attempts: s.attempts,
          error: s.error,
          message: s.message ?? null,
        }),
      );
    }

    case "retry":
      return {
        ...state,
        phase: "transferring",
        files: state.files.map((f) =>
          action.keys.includes(f.key)
            ? {
                ...f,
                stage: "waiting",
                uploadPercent: 0,
                error: null,
                retryable: false,
              }
            : f,
        ),
      };

    case "reset":
      return initialPipelineState;
  }
}

// ---------------------------------------------------------------------------
// Hook
// ---------------------------------------------------------------------------

interface RunContext {
  jobId: string;
  userId: string;
  mediaType: "MOVIE" | "SHOW";
  files: Map<string, PlannedFile>;
  /** file key -> tmdb id the backend expects (movie id, or episode id) */
  tmdbIds: Map<string, number>;
}

export function useUploadPipeline() {
  const [state, dispatch] = useReducer(reducer, initialPipelineState);
  /** true while the status websocket is connected */
  const [live, setLive] = useState(false);

  const runRef = useRef(0); // bumped on start/reset/unmount so stale loops stop
  const ctxRef = useRef<RunContext | null>(null);
  const stateRef = useRef(state);
  stateRef.current = state;

  // fileUploadId -> file key, filled in as each PUT returns
  const idToKey = useRef(new Map<string, string>());
  // The server can push a file's first status before its PUT response has been
  // handled here (the file is queued as soon as it's saved). Hold those until
  // we know which row they belong to.
  const pendingStatus = useRef(new Map<string, FileStatusResponse>());

  const applyStatus = useCallback((s: FileStatusResponse) => {
    const key = idToKey.current.get(s.file_upload_id);
    if (!key) {
      pendingStatus.current.set(s.file_upload_id, s);
      return;
    }
    dispatch({ type: "server-status", key, status: s });
  }, []);

  useEffect(() => {
    return () => {
      runRef.current++;
    };
  }, []);

  // ---- sequential file uploads (one HTTP call at a time) ----
  const uploadFiles = useCallback(async (keys: string[], run: number) => {
    const ctx = ctxRef.current;
    if (!ctx) return;

    for (const key of keys) {
      if (runRef.current !== run) return;

      const planned = ctx.files.get(key);
      const tmdbId = ctx.tmdbIds.get(key);
      if (!planned || tmdbId === undefined) continue;

      dispatch({ type: "upload-started", key });
      try {
        const res = await uploadFile({
          jobId: ctx.jobId,
          userId: ctx.userId,
          mediaType: ctx.mediaType,
          tmdbId,
          language: planned.language,
          file: planned.file,
          onProgress: (percent) => {
            if (runRef.current === run)
              dispatch({ type: "upload-progress", key, percent });
          },
        });
        if (runRef.current !== run) return;

        idToKey.current.set(res.file_upload_id, key);
        dispatch({
          type: "uploaded",
          key,
          fileUploadId: res.file_upload_id,
          status: res.status,
        });

        // apply anything the socket delivered before this response did
        const early = pendingStatus.current.get(res.file_upload_id);
        if (early) {
          pendingStatus.current.delete(res.file_upload_id);
          dispatch({ type: "server-status", key, status: early });
        }
      } catch (e) {
        if (runRef.current !== run) return;
        dispatch({
          type: "upload-failed",
          key,
          error: e instanceof Error ? e.message : "Upload failed",
          retryable: true,
        });
      }
    }
  }, []);

  // ---- the one-click entry point ----
  const start = useCallback(
    async ({ built, userId }: { built: BuiltLike; userId: string }) => {
      const run = ++runRef.current;
      const files = planFromBuilt(built);

      idToKey.current.clear();
      pendingStatus.current.clear();

      if (files.length === 0) {
        dispatch({ type: "fail", error: "There are no files to upload." });
        return;
      }

      const mediaType = built.metadata.type;
      dispatch({ type: "start", files });

      try {
        // 1. create the job
        const jobId = await createUploadJob({
          tmdbId: built.metadata.tmdbId,
          mediaType,
          userId,
        });
        if (runRef.current !== run) return;
        dispatch({ type: "job-created", jobId });

        // 2. wait for catalog validation (the backend rejects uploads until then)
        const job = await waitForValidation(
          jobId,
          userId,
          () => runRef.current !== run,
        );
        if (runRef.current !== run) return;

        // 3. work out which TMDB id each file belongs to
        const episodeIds = new Map<string, number>(
          job.episodes.map(
            (e) =>
              [`${e.season_number}:${e.episode_number}`, e.tmdb_id] as [
                string,
                number,
              ],
          ),
        );
        const tmdbIds = new Map<string, number>();
        const unresolved: PlannedFile[] = [];
        for (const f of files) {
          const id =
            mediaType === "MOVIE"
              ? job.tmdb_id
              : episodeIds.get(`${f.seasonNumber}:${f.episodeNumber}`);
          if (id === undefined) unresolved.push(f);
          else tmdbIds.set(f.key, id);
        }

        ctxRef.current = {
          jobId,
          userId,
          mediaType,
          files: new Map(files.map((f) => [f.key, f])),
          tmdbIds,
        };

        // moves to "transferring", which is also what opens the status socket
        dispatch({ type: "validated", title: job.title });
        unresolved.forEach((f) =>
          dispatch({
            type: "upload-failed",
            key: f.key,
            error: "No matching episode found on TMDB for this show",
            retryable: false,
          }),
        );

        // 4. upload the rest, one at a time
        await uploadFiles(
          files.filter((f) => tmdbIds.has(f.key)).map((f) => f.key),
          run,
        );
      } catch (e) {
        if (e instanceof CancelledError || runRef.current !== run) return;
        dispatch({
          type: "fail",
          error: e instanceof Error ? e.message : "Upload failed",
        });
      }
    },
    [uploadFiles],
  );

  const retryFailedUploads = useCallback(() => {
    const ctx = ctxRef.current;
    if (!ctx) return;
    const keys = stateRef.current.files
      .filter(
        (f) =>
          f.stage === "upload-failed" && f.retryable && ctx.tmdbIds.has(f.key),
      )
      .map((f) => f.key);
    if (keys.length === 0) return;

    dispatch({ type: "retry", keys });
    void uploadFiles(keys, runRef.current);
  }, [uploadFiles]);

  const reset = useCallback(() => {
    runRef.current++;
    ctxRef.current = null;
    idToKey.current.clear();
    pendingStatus.current.clear();
    dispatch({ type: "reset" });
  }, []);

  // ---- live server-side status over a websocket ----
  // Open while files are being transferred / processed. On connect (and on every
  // reconnect) the server first sends a snapshot of all the job's files, so a
  // dropped connection never leaves the UI stale.
  useEffect(() => {
    if (state.phase !== "transferring" || !state.jobId) return;
    const ctx = ctxRef.current;
    if (!ctx) return;

    const url = `${WS_URL_ROOT}/upload-pipeline/${state.jobId}?userId=${encodeURIComponent(ctx.userId)}`;

    let closed = false;
    let socket: WebSocket | null = null;
    let retryTimer: ReturnType<typeof setTimeout> | undefined;
    let attempt = 0;

    const connect = () => {
      socket = new WebSocket(url);

      socket.onopen = () => {
        attempt = 0;
        setLive(true);
      };

      socket.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data) as PipelineSocketMessage;
          if (msg.type === "snapshot") msg.files.forEach(applyStatus);
          else if (msg.type === "file") applyStatus(msg.file);
        } catch {
          // ignore malformed payloads rather than breaking the panel
        }
      };

      socket.onclose = (event) => {
        setLive(false);
        if (closed) return;
        // The server closes with 1007 (bad job id / userId) or 1008 (job missing
        // or not yours) and will refuse again, so don't loop on those.
        if (event.code === 1007 || event.code === 1008) return;
        retryTimer = setTimeout(
          connect,
          Math.min(1000 * 2 ** attempt++, 10_000),
        );
      };
    };

    connect();

    return () => {
      closed = true;
      if (retryTimer) clearTimeout(retryTimer);
      socket?.close();
      setLive(false);
    };
  }, [state.phase, state.jobId, applyStatus]);

  const isRunning =
    state.phase === "creating-job" ||
    state.phase === "validating" ||
    state.phase === "transferring";

  const canRetry =
    state.phase === "finished" &&
    state.files.some((f) => f.stage === "upload-failed" && f.retryable);

  return {
    state,
    live,
    start,
    retryFailedUploads,
    reset,
    isRunning,
    canRetry,
  };
}
