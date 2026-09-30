export type FileRole = "video" | "subtitle" | "other";

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
}

export interface SeasonUpload {
  seasonNumber: number;
  folderName: string;
  episodes: EpisodeUpload[];
}

export interface ShowStructure {
  seasons: SeasonUpload[];
  /** Top-level files not inside any folder — invalid placement for a show, skipped. */
  looseFiles: TreeFileNode[];
  canonicalNameById: Map<string, string>;
  seasonLabelByFolderId: Map<string, string>;
}

export interface MovieStructure {
  video: TreeFileNode | null;
  subtitles: SubtitleUpload[];
  extraVideos: TreeFileNode[];
  hasFolders: boolean;
}

export type TreeNode = TreeFileNode | TreeFolderNode;
