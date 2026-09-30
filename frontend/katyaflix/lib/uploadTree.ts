import { DroppedFile } from "./files";
import { isVideoFile } from "./video";
import { subtitleExt } from "./subtitles";
import { guessLanguageCode } from "./languageGuess";
import { naturalCompare } from "./naturalSort";
import {
  EpisodeUpload,
  FileRole,
  MovieStructure,
  SeasonUpload,
  ShowStructure,
  SubtitleUpload,
  TreeFileNode,
  TreeFolderNode,
} from "@/types/uploadTree";

function fileRole(name: string): FileRole {
  if (isVideoFile(name)) return "video";
  if (subtitleExt(name) !== "other") return "subtitle";
  return "other";
}

function extOf(name: string): string {
  const m = name.match(/\.[^./]+$/);
  return m ? m[0].toLowerCase() : "";
}

function stemOf(name: string): string {
  return name.replace(/\.[^./]+$/, "");
}

function pad2(n: number): string {
  return String(n).padStart(2, "0");
}

/** Builds a nested tree that faithfully mirrors whatever folder structure was dropped. */
export function buildTree(files: DroppedFile[]): TreeFolderNode {
  const root: TreeFolderNode = {
    kind: "folder",
    id: "",
    name: "",
    children: [],
  };

  for (const dropped of files) {
    const segments = dropped.relativePath.split("/").filter(Boolean);
    let current = root;
    let pathSoFar = "";

    segments.forEach((seg, i) => {
      pathSoFar = pathSoFar ? `${pathSoFar}/${seg}` : seg;
      const isLast = i === segments.length - 1;

      if (isLast) {
        current.children.push({
          kind: "file",
          id: pathSoFar,
          name: seg,
          file: dropped.file,
          role: fileRole(seg),
        });
        return;
      }

      let next = current.children.find(
        (c): c is TreeFolderNode => c.kind === "folder" && c.name === seg,
      );
      if (!next) {
        next = { kind: "folder", id: pathSoFar, name: seg, children: [] };
        current.children.push(next);
      }
      current = next;
    });
  }

  sortTree(root);
  return root;
}

function sortTree(node: TreeFolderNode) {
  node.children.sort((a, b) => {
    if (a.kind !== b.kind) return a.kind === "folder" ? -1 : 1;
    return naturalCompare(a.name, b.name);
  });
  node.children.forEach((child) => {
    if (child.kind === "folder") sortTree(child);
  });
}

function collectDescendantFiles(node: TreeFolderNode): TreeFileNode[] {
  const out: TreeFileNode[] = [];
  for (const child of node.children) {
    if (child.kind === "file") out.push(child);
    else out.push(...collectDescendantFiles(child));
  }
  return out;
}

export function computeShowStructure(tree: TreeFolderNode): ShowStructure {
  const topFolders = tree.children.filter(
    (c): c is TreeFolderNode => c.kind === "folder",
  );
  const looseFiles = tree.children.filter(
    (c): c is TreeFileNode => c.kind === "file",
  );
  const sortedFolders = [...topFolders].sort((a, b) =>
    naturalCompare(a.name, b.name),
  );

  const seasons: SeasonUpload[] = [];
  const canonicalNameById = new Map<string, string>();
  const seasonLabelByFolderId = new Map<string, string>();

  sortedFolders.forEach((folder, folderIndex) => {
    const seasonNumber = folderIndex + 1;
    seasonLabelByFolderId.set(folder.id, `Season ${seasonNumber}`);

    const descendants = collectDescendantFiles(folder);
    const videos = descendants
      .filter((f) => f.role === "video")
      .sort((a, b) => naturalCompare(a.name, b.name));
    const subtitleFiles = descendants.filter((f) => f.role === "subtitle");

    const episodes: EpisodeUpload[] = videos.map((video, videoIndex) => {
      const episodeNumber = videoIndex + 1;
      const base = `S${pad2(seasonNumber)}EP${pad2(episodeNumber)}`;
      canonicalNameById.set(video.id, `${base}${extOf(video.name)}`);

      const videoStem = stemOf(video.name).toLowerCase();
      const matchedSubs = subtitleFiles.filter((sub) => {
        const subStem = stemOf(sub.name).toLowerCase();
        return (
          subStem === videoStem ||
          subStem.includes(videoStem) ||
          videoStem.includes(subStem)
        );
      });

      const subtitles: SubtitleUpload[] = matchedSubs.map((node) => {
        const { code, guessed } = guessLanguageCode(node.name);
        return { node, languageCode: code, guessed };
      });

      const multiple = subtitles.length > 1;
      subtitles.forEach((sub) => {
        const name = multiple
          ? `${base}.${sub.languageCode}${extOf(sub.node.name)}`
          : `${base}${extOf(sub.node.name)}`;
        canonicalNameById.set(sub.node.id, name);
      });

      return { seasonNumber, episodeNumber, video, subtitles };
    });

    seasons.push({ seasonNumber, folderName: folder.name, episodes });
  });

  return { seasons, looseFiles, canonicalNameById, seasonLabelByFolderId };
}

// ----------------------------------------------------------------------------
// Movies: exactly one top-level video file, plus any number of top-level
// subtitle files. Folders aren't expected at all. Movies are not renamed.
// ----------------------------------------------------------------------------

export function computeMovieStructure(tree: TreeFolderNode): MovieStructure {
  const hasFolders = tree.children.some((c) => c.kind === "folder");
  const files = tree.children.filter(
    (c): c is TreeFileNode => c.kind === "file",
  );
  const videos = files.filter((f) => f.role === "video");
  const subtitleNodes = files.filter((f) => f.role === "subtitle");

  const subtitles: SubtitleUpload[] = subtitleNodes.map((node) => {
    const { code, guessed } = guessLanguageCode(node.name);
    return { node, languageCode: code, guessed };
  });

  return {
    video: videos[0] ?? null,
    subtitles,
    extraVideos: videos.slice(1),
    hasFolders,
  };
}
