"use client";

import { useMemo, useState } from "react";
import Link from "next/link";

import TmdbSearch from "@/components/TmdbSearch";
import UploadDropzone from "@/components/UploadDropzone";
import FileTree from "@/components/FileTree";
import UploadJobStatusPanel from "@/components/UploadJobStatusPanel";

import { DroppedFile } from "@/lib/files";
import {
  buildTree,
  computeMovieStructure,
  computeShowStructure,
} from "@/lib/uploadTree";
import { buildMovieUpload, buildShowUpload } from "@/types/uploadPayload";
import { TmdbSearchResult } from "@/types/tmdb";

import { useTmdbDetails } from "@/hooks/useTmdbDetails";
import { useUpload } from "@/hooks/useUpload";
import { useUser } from "@/context/UserContext";

type MediaType = "movie" | "show";

export default function UploadPage() {
  const user = useUser();
  const [mediaType, setMediaType] = useState<MediaType>("movie");
  const [selected, setSelected] = useState<TmdbSearchResult | null>(null);

  const [droppedFiles, setDroppedFiles] = useState<DroppedFile[]>([]);
  const [progress, setProgress] = useState(0);

  const tmdbType = mediaType === "show" ? "tv" : "movie";

  const { data: details, isLoading: detailsLoading } = useTmdbDetails(
    tmdbType,
    selected?.id ?? null,
  );

  const uploadMutation = useUpload();

  const isUploading = uploadMutation.isPending;

  const uploadError = uploadMutation.error
    ? uploadMutation.error instanceof Error
      ? uploadMutation.error.message
      : "Upload failed."
    : null;

  const uploadSuccess = uploadMutation.data
    ? { jobId: uploadMutation.data.jobId }
    : null;

  function switchMediaType(next: MediaType) {
    setMediaType(next);
    setSelected(null);
    setProgress(0);
    uploadMutation.reset();
  }

  function handleFiles(incoming: DroppedFile[]) {
    setDroppedFiles((prev) => {
      const map = new Map(prev.map((f) => [f.relativePath, f]));

      for (const f of incoming) {
        map.set(f.relativePath, f);
      }

      return Array.from(map.values());
    });

    setProgress(0);
    uploadMutation.reset();
  }

  const tree = useMemo(() => buildTree(droppedFiles), [droppedFiles]);

  const movieStructure = useMemo(() => computeMovieStructure(tree), [tree]);

  const showStructure = useMemo(() => computeShowStructure(tree), [tree]);

  const ignoredIds = useMemo(() => {
    if (mediaType === "movie") {
      return new Set(movieStructure.extraVideos.map((v) => v.id));
    }

    return new Set(showStructure.looseFiles.map((f) => f.id));
  }, [mediaType, movieStructure, showStructure]);

  const validation = useMemo(() => {
    if (droppedFiles.length === 0) {
      return {
        ready: false,
        message: null as string | null,
      };
    }

    if (mediaType === "movie") {
      if (movieStructure.hasFolders) {
        return {
          ready: false,
          message:
            "Movies expect a single video file, not folders — switch to Show for a series.",
        };
      }

      if (!movieStructure.video) {
        return {
          ready: false,
          message: "Drop a video file to continue.",
        };
      }

      if (movieStructure.extraVideos.length > 0) {
        return {
          ready: false,
          message: `Found ${
            movieStructure.extraVideos.length + 1
          } video files — movies expect exactly one.`,
        };
      }

      return {
        ready: true,
        message: null,
      };
    }

    const totalEpisodes = showStructure.seasons.reduce(
      (n, s) => n + s.episodes.length,
      0,
    );

    if (totalEpisodes === 0) {
      return {
        ready: false,
        message:
          "Drop one folder per season, each containing its episode files.",
      };
    }

    return {
      ready: true,
      message: null,
    };
  }, [mediaType, droppedFiles, movieStructure, showStructure]);

  const canUpload = validation.ready && Boolean(selected) && !isUploading;

  async function handleUpload() {
    if (!selected) return;

    setProgress(0);
    uploadMutation.reset();

    const built =
      mediaType === "movie"
        ? buildMovieUpload(selected, movieStructure)
        : buildShowUpload(selected, showStructure);

    if (!built) {
      return;
    }

    try {
      await uploadMutation.mutateAsync({
        formData: built.formData,
        userId: user.user?.id || "n/a",
        onProgress: setProgress,
      });

      setDroppedFiles([]);
    } catch {
      // Error is exposed through uploadMutation.error.
    }
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-12 ">
      <div className="flex items-center justify-between">
        <div>
          <p className="eyebrow">Add to library</p>

          <h1 className="mt-2 font-display text-4xl font-black uppercase tracking-tightest text-paper">
            Upload
          </h1>
        </div>

        <Link
          href="/jobs"
          className="font-mono text-xs uppercase tracking-widest text-paper-muted hover:text-paper"
        >
          My Upload Jobs →
        </Link>
      </div>

      <div className="mt-8 grid gap-8 lg:grid-cols-[340px_1fr]">
        {/* Sidebar: type, title match, details, upload control */}
        <div className="space-y-6">
          <div>
            <p className="ticket-tag">Type</p>

            <div className="mt-2 inline-flex rounded border border-ink-line p-1">
              {(["movie", "show"] as const).map((t) => (
                <label
                  key={t}
                  className={`cursor-pointer rounded px-4 py-1.5 font-mono text-xs uppercase tracking-widest transition-colors ${
                    mediaType === t
                      ? "bg-marquee text-ink"
                      : "text-paper-muted hover:text-paper"
                  }`}
                >
                  <input
                    type="radio"
                    name="mediaType"
                    value={t}
                    checked={mediaType === t}
                    onChange={() => switchMediaType(t)}
                    className="sr-only"
                  />

                  {t === "movie" ? "Movie" : "Show"}
                </label>
              ))}
            </div>
          </div>

          <div>
            <p className="ticket-tag">Title</p>

            <div className="mt-2">
              <TmdbSearch
                type={mediaType === "show" ? "tv" : "movie"}
                selected={selected}
                onSelect={setSelected}
              />
            </div>
          </div>

          {selected && (
            <div className="rounded-lg border border-ink-line bg-ink-raised p-4">
              {detailsLoading && (
                <p className="font-mono text-xs text-paper-muted">
                  Loading details…
                </p>
              )}

              {!detailsLoading && details && (
                <div className="space-y-3">
                  <p className="text-sm text-paper-muted">
                    {details.overview || "No synopsis available."}
                  </p>

                  {details.creators.length > 0 && (
                    <p className="font-mono text-xs text-paper-muted">
                      {mediaType === "show" ? "Created by" : "Directed by"}:{" "}
                      <span className="text-paper">
                        {details.creators.join(", ")}
                      </span>
                    </p>
                  )}

                  {details.genres.length > 0 && (
                    <div className="flex flex-wrap gap-1.5">
                      {details.genres.map((g) => (
                        <span
                          key={g}
                          className="rounded-full border border-ink-line px-2 py-0.5 font-mono text-[10px] uppercase tracking-widest text-paper-muted"
                        >
                          {g}
                        </span>
                      ))}
                    </div>
                  )}

                  {mediaType === "movie" && details.runtime && (
                    <p className="font-mono text-xs text-paper-muted">
                      Runtime:{" "}
                      <span className="text-paper">{details.runtime} min</span>
                    </p>
                  )}

                  {mediaType === "show" && details.status && (
                    <p className="font-mono text-xs text-paper-muted">
                      Status:{" "}
                      <span className="text-paper">{details.status}</span>
                    </p>
                  )}
                </div>
              )}
            </div>
          )}

          <div className="sprocket-rule" />

          <div>
            <button
              type="button"
              disabled={!canUpload}
              onClick={handleUpload}
              className="w-full rounded bg-marquee py-3 font-mono text-sm font-semibold uppercase tracking-widest text-ink transition-colors hover:bg-marquee-bright disabled:cursor-not-allowed  disabled:bg-ink-line disabled:text-paper-faint"
            >
              {isUploading ? `Uploading… ${progress}%` : "Upload"}
            </button>

            {isUploading && (
              <div className="mt-3 h-1.5 w-full overflow-hidden rounded-full bg-ink-line">
                <div
                  className="h-full rounded-full bg-marquee transition-all"
                  style={{ width: `${progress}%` }}
                />
              </div>
            )}

            {validation.message && !isUploading && (
              <p className="mt-3 font-mono text-xs text-paper-muted">
                {validation.message}
              </p>
            )}

            {!selected && droppedFiles.length > 0 && (
              <p className="mt-3 font-mono text-xs text-paper-muted">
                Match a title on TMDB to continue.
              </p>
            )}

            {uploadError && (
              <p className="mt-3 font-mono text-xs text-signal-error">
                {uploadError}
              </p>
            )}

            {/*
              Files finish uploading well before encoding/finalizing does -
              the job is now queued and processed in the background, so we
              switch to a live websocket panel here instead of a static
              "uploaded" message.
            */}
            {uploadSuccess && (
              <div className="mt-3">
                <UploadJobStatusPanel jobId={uploadSuccess.jobId} />
              </div>
            )}
          </div>
        </div>

        {/* Main: dropzone + hierarchy preview */}
        <div>
          <UploadDropzone onFiles={handleFiles} />

          {droppedFiles.length > 0 && (
            <div className="mt-4 rounded-lg border border-ink-line bg-ink-raised">
              <div className="flex items-center justify-between border-b border-ink-line px-4 py-2">
                <p className="font-mono text-xs text-paper-muted">
                  {droppedFiles.length} file(s)
                </p>

                <button
                  type="button"
                  onClick={() => {
                    setDroppedFiles([]);
                    setProgress(0);
                    uploadMutation.reset();
                  }}
                  className="font-mono text-xs uppercase tracking-widest text-paper-muted hover:text-signal-error"
                >
                  Clear all
                </button>
              </div>

              <div className="max-h-96 overflow-y-auto p-2">
                <FileTree
                  node={tree}
                  canonicalNameById={
                    mediaType === "show"
                      ? showStructure.canonicalNameById
                      : undefined
                  }
                  seasonLabelByFolderId={
                    mediaType === "show"
                      ? showStructure.seasonLabelByFolderId
                      : undefined
                  }
                  ignoredIds={ignoredIds}
                />
              </div>
            </div>
          )}

          {mediaType === "show" && showStructure.looseFiles.length > 0 && (
            <p className="mt-3 font-mono text-xs text-signal-error">
              {showStructure.looseFiles.length} file(s) aren&rsquo;t inside a
              season folder and will be skipped.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
