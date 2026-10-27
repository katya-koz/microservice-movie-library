import type { FileKind, PlannedFile } from "@/types/uploadPipeline";

// Structural types: the existing BuiltUpload from buildMovieUpload/buildShowUpload
// is assignable to this, so those builders don't need to change (apart from adding `language`).
interface EntryLike {
  key: string;
  fileType: FileKind;
  filename: string;
  language?: string;
}

export interface BuiltLike {
  metadata:
    | { type: "MOVIE"; tmdbId: number; files: EntryLike[] }
    | {
        type: "SHOW";
        tmdbId: number;
        seasons: {
          seasonNumber: number;
          episodes: { episodeNumber: number; files: EntryLike[] }[];
        }[];
      };
  formData: FormData;
}

function pad2(n: number): string {
  return String(n).padStart(2, "0");
}

export function planFromBuilt(built: BuiltLike): PlannedFile[] {
  const out: PlannedFile[] = [];

  const add = (
    entry: EntryLike,
    baseLabel: string,
    seasonNumber?: number,
    episodeNumber?: number,
  ) => {
    const file = built.formData.get(entry.key);
    if (!(file instanceof File)) return;

    const isVideo = entry.fileType === "VIDEO";
    out.push({
      key: entry.key,
      filename: entry.filename,
      fileType: entry.fileType,
      label: isVideo
        ? baseLabel
        : `${baseLabel} subtitle${entry.language ? ` (${entry.language})` : ""}`,
      seasonNumber,
      episodeNumber,
      language: isVideo ? undefined : entry.language,
      file,
    });
  };

  const { metadata } = built;

  if (metadata.type === "MOVIE") {
    metadata.files.forEach((entry) => add(entry, "Movie"));
  } else {
    for (const season of metadata.seasons) {
      for (const episode of season.episodes) {
        const label = `S${pad2(season.seasonNumber)}EP${pad2(episode.episodeNumber)}`;
        episode.files.forEach((entry) =>
          add(entry, label, season.seasonNumber, episode.episodeNumber),
        );
      }
    }
  }

  return out;
}
