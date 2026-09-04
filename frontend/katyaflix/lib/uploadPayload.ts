import { TmdbSearchResult } from "./types";
import { MovieStructure, ShowStructure } from "./uploadTree";

export type FileType = "VIDEO" | "SUBTITLE";

export interface UploadFileEntryDto {
  key: string;
  fileType: FileType;
  filename: string;
}

export interface EpisodeUploadMetadataDto {
  episodeNumber: number;
  files: UploadFileEntryDto[];
}

export interface SeasonUploadMetadataDto {
  seasonNumber: number;
  episodes: EpisodeUploadMetadataDto[];
}

export interface MovieUploadMetadataDto {
  type: "MOVIE";
  tmdbId: number;
  files: UploadFileEntryDto[];
}

export interface ShowUploadMetadataDto {
  type: "SHOW";
  tmdbId: number;
  seasons: SeasonUploadMetadataDto[];
}

export type UploadMetadata = MovieUploadMetadataDto | ShowUploadMetadataDto;

export interface BuiltUpload {
  metadata: UploadMetadata;
  formData: FormData;
}

export function buildMovieUpload(
  selected: TmdbSearchResult,
  structure: MovieStructure,
): BuiltUpload | null {
  if (!structure.video) return null;

  const files: UploadFileEntryDto[] = [
    { key: "video", fileType: "VIDEO", filename: structure.video.name },
    ...structure.subtitles.map((sub, i) => ({
      key: `sub_${i}`,
      fileType: "SUBTITLE" as const,
      filename: sub.node.name, // movies aren't renamed
    })),
  ];

  const metadata: MovieUploadMetadataDto = {
    type: "MOVIE",
    tmdbId: selected.id,
    files,
  };

  const formData = new FormData();
  formData.append("metadata", JSON.stringify(metadata));
  formData.append("video", structure.video.file, structure.video.name);
  structure.subtitles.forEach((sub, i) =>
    formData.append(`sub_${i}`, sub.node.file, sub.node.name),
  );

  return { metadata, formData };
}

export function buildShowUpload(
  selected: TmdbSearchResult,
  structure: ShowStructure,
): BuiltUpload | null {
  const seasonsWithEpisodes = structure.seasons.filter(
    (s) => s.episodes.length > 0,
  );
  if (seasonsWithEpisodes.length === 0) return null;

  const formData = new FormData();

  const seasons: SeasonUploadMetadataDto[] = seasonsWithEpisodes.map(
    (season) => ({
      seasonNumber: season.seasonNumber,
      episodes: season.episodes.map((episode) => {
        const videoKey = `s${season.seasonNumber}_e${episode.episodeNumber}_video`;
        formData.append(videoKey, episode.video.file, episode.video.name);

        const files: UploadFileEntryDto[] = [
          {
            key: videoKey,
            fileType: "VIDEO",
            filename:
              structure.canonicalNameById.get(episode.video.id) ??
              episode.video.name,
          },
          ...episode.subtitles.map((sub, i) => {
            const subKey = `${videoKey}_sub${i}`;
            formData.append(subKey, sub.node.file, sub.node.name);
            return {
              key: subKey,
              fileType: "SUBTITLE" as const,
              filename:
                structure.canonicalNameById.get(sub.node.id) ?? sub.node.name,
            };
          }),
        ];

        return { episodeNumber: episode.episodeNumber, files };
      }),
    }),
  );

  const metadata: ShowUploadMetadataDto = {
    type: "SHOW",
    tmdbId: selected.id,
    seasons,
  };
  formData.append("metadata", JSON.stringify(metadata));
  return { metadata, formData };
}
