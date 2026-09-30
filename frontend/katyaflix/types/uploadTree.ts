export type FileRole = "video" | "subtitle" | "other";
export type NumberSource = "override" | "parsed" | "sequential";

export interface ShowOverrides {
  seasons: Record<string, number>; // season folder id -> season number
  episodes: Record<string, number>; // video file id -> episode number
}
export interface TreeFileNode {
  kind: "file";
  id: string; // = relativePath, stable across renders
  name: string;
  file: File;
  role: FileRole;
}

export interface TreeFolderNode {
  kind: "folder";
  id: string;
  name: string;
  children: TreeNode[];
}

export interface SubtitleUpload {
  node: TreeFileNode;
  languageCode: string;
  guessed: boolean;
}

export interface EpisodeUpload {
  seasonNumber: number;
  episodeNumber: number;
  video: TreeFileNode;
  subtitles: SubtitleUpload[];
  source: NumberSource;
  conflict: boolean;
}

export interface SeasonUpload {
  seasonNumber: number;
  folderName: string;
  episodes: EpisodeUpload[];
  folderId: string;
  source: NumberSource;
  conflict: boolean;
}

export interface ShowStructure {
  seasons: SeasonUpload[];
  looseFiles: TreeFileNode[];
  canonicalNameById: Map<string, string>;
  seasonLabelByFolderId: Map<string, string>;
  droppedSubtitleIds: Set<string>;
}

export interface MovieStructure {
  video: TreeFileNode | null;
  subtitles: SubtitleUpload[];
  extraVideos: TreeFileNode[];
  hasFolders: boolean;
}

export type TreeNode = TreeFileNode | TreeFolderNode;
