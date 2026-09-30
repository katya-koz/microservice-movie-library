import { DroppedFile } from "./files";
import { isVideoFile } from "./video";
import { subtitleExt } from "./subtitles";
import { guessLanguageCode } from "./languageGuess";
import { naturalCompare } from "./naturalSort";
import {
  EpisodeUpload,
  FileRole,
  MovieStructure,
  NumberSource,
  SeasonUpload,
  ShowOverrides,
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

// ----------------------------------------------------------------------------
// Season / episode number detection
// ----------------------------------------------------------------------------

const SE_PATTERNS = [
  /(?:^|[^a-z])s(\d{1,2})[\s._-]*e(?:p)?(\d{1,3})/i, // S01E02, S01.EP02
  /(?:^|[^\d])(\d{1,2})x(\d{2,3})(?!\d)/i, // 1x02
];

function parseSeasonEpisode(
  name: string,
): { season: number; episode: number } | null {
  const stem = stemOf(name);
  for (const re of SE_PATTERNS) {
    const m = stem.match(re);
    if (m) return { season: Number(m[1]), episode: Number(m[2]) };
  }
  return null;
}

function parseSeasonFromFolderName(name: string): number | null {
  const m = name.match(
    /(?:^|[^a-z])(?:season|series|s)[\s._-]*(\d{1,2})(?!\d)/i,
  );
  return m ? Number(m[1]) : null;
}

/** Folder name first, then the majority season found in its video filenames. */
function detectSeason(
  folder: TreeFolderNode,
  videos: TreeFileNode[],
): number | null {
  const fromName = parseSeasonFromFolderName(folder.name);
  if (fromName !== null) return fromName;

  const counts = new Map<number, number>();
  for (const v of videos) {
    const se = parseSeasonEpisode(v.name);
    if (se) counts.set(se.season, (counts.get(se.season) ?? 0) + 1);
  }
  let best: number | null = null;
  let bestCount = 0;
  for (const [season, count] of counts) {
    if (count > bestCount) {
      best = season;
      bestCount = count;
    }
  }
  return best;
}

/** Sequential fallback that never reuses a number already claimed explicitly. */
function makeFallback(used: Set<number>, start: number) {
  let n = start;
  return () => {
    while (used.has(n)) n++;
    used.add(n);
    return n;
  };
}

function markConflicts<T>(items: T[], numberOf: (t: T) => number) {
  const counts = new Map<number, number>();
  items.forEach((i) =>
    counts.set(numberOf(i), (counts.get(numberOf(i)) ?? 0) + 1),
  );
  return (item: T) => (counts.get(numberOf(item)) ?? 0) > 1;
}

/** Looks for SxxExx in a file's own name, then each parent folder name (nearest first). */
function parseSeasonEpisodeFromPath(
  path: string,
): { season: number; episode: number } | null {
  const segments = path.split("/").filter(Boolean).reverse();
  for (const seg of segments) {
    const se = parseSeasonEpisode(seg);
    if (se) return se;
  }
  return null;
}

// ----------------------------------------------------------------------------
// Tree
// ----------------------------------------------------------------------------

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

// ----------------------------------------------------------------------------
// Shows
//
// Season number:  override > folder name / filenames > lowest unused (sequential)
// Episode number: override > parsed from filename    > lowest unused (sequential)
// ----------------------------------------------------------------------------

export function computeShowStructure(
  tree: TreeFolderNode,
  overrides: ShowOverrides = { seasons: {}, episodes: {} },
): ShowStructure {
  const topFolders = tree.children.filter(
    (c): c is TreeFolderNode => c.kind === "folder",
  );
  const looseFiles = tree.children.filter(
    (c): c is TreeFileNode => c.kind === "file",
  );
  const sortedFolders = [...topFolders].sort((a, b) =>
    naturalCompare(a.name, b.name),
  );

  const canonicalNameById = new Map<string, string>();
  const seasonLabelByFolderId = new Map<string, string>();
  const droppedSubtitleIds = new Set<string>();

  // ---- Pass 1: resolve season numbers ----
  const folderInfo = sortedFolders.map((folder) => {
    const videos = collectDescendantFiles(folder)
      .filter((f) => f.role === "video")
      .sort((a, b) => naturalCompare(a.name, b.name));
    return { folder, videos };
  });

  const resolvedSeason = new Map<string, { n: number; source: NumberSource }>();
  for (const { folder, videos } of folderInfo) {
    const o = overrides.seasons[folder.id];
    if (o !== undefined) {
      resolvedSeason.set(folder.id, { n: o, source: "override" });
      continue;
    }
    const parsed = detectSeason(folder, videos);
    if (parsed !== null) {
      resolvedSeason.set(folder.id, { n: parsed, source: "parsed" });
    }
  }
  const nextSeason = makeFallback(
    new Set([...resolvedSeason.values()].map((v) => v.n)),
    1,
  );
  for (const { folder } of folderInfo) {
    if (!resolvedSeason.has(folder.id)) {
      resolvedSeason.set(folder.id, { n: nextSeason(), source: "sequential" });
    }
  }

  // ---- Pass 2: episodes within each season ----
  const seasons: SeasonUpload[] = folderInfo.map(({ folder, videos }) => {
    const { n: seasonNumber, source: seasonSource } = resolvedSeason.get(
      folder.id,
    )!;
    seasonLabelByFolderId.set(folder.id, `Season ${seasonNumber}`);

    const subtitleFiles = collectDescendantFiles(folder).filter(
      (f) => f.role === "subtitle",
    );

    const explicit = new Map<string, { n: number; source: NumberSource }>();
    for (const v of videos) {
      const o = overrides.episodes[v.id];
      if (o !== undefined) {
        explicit.set(v.id, { n: o, source: "override" });
        continue;
      }
      const se = parseSeasonEpisode(v.name);
      if (se) explicit.set(v.id, { n: se.episode, source: "parsed" });
    }
    // Duplicates from parsing are kept (and flagged) so the user can see and fix them.
    const nextEpisode = makeFallback(
      new Set([...explicit.values()].map((v) => v.n)),
      1,
    );

    const resolved = videos.map((video) => {
      const hit = explicit.get(video.id) ?? {
        n: nextEpisode(),
        source: "sequential" as NumberSource,
      };
      return { video, episodeNumber: hit.n, source: hit.source };
    });
    resolved.sort((a, b) => a.episodeNumber - b.episodeNumber); // stable
    const isConflict = markConflicts(resolved, (r) => r.episodeNumber);

    const episodes: EpisodeUpload[] = resolved.map((r) => {
      const { video, episodeNumber } = r;
      const base = `S${pad2(seasonNumber)}EP${pad2(episodeNumber)}`;
      canonicalNameById.set(video.id, `${base}${extOf(video.name)}`);

      const videoSE = parseSeasonEpisode(video.name);
      const videoStem = stemOf(video.name).toLowerCase();

      const matchedSubs = subtitleFiles.filter((sub) => {
        // Prefer SxxExx found in the subtitle's path, e.g.
        // Subs/<episode folder>/2_English.srt
        const subSE = parseSeasonEpisodeFromPath(sub.id);
        if (subSE && videoSE) {
          return (
            subSE.season === videoSE.season && subSE.episode === videoSE.episode
          );
        }
        // Otherwise fall back to filename similarity
        const subStem = stemOf(sub.name).toLowerCase();
        return (
          subStem === videoStem ||
          subStem.includes(videoStem) ||
          videoStem.includes(subStem)
        );
      });

      const allSubtitles: SubtitleUpload[] = matchedSubs.map((node) => {
        const { code, guessed } = guessLanguageCode(node.name);
        return { node, languageCode: code, guessed };
      });

      // One subtitle per language: keep the largest (usually the full dialogue).
      const byLang = new Map<string, SubtitleUpload>();
      for (const s of allSubtitles) {
        const cur = byLang.get(s.languageCode);
        if (!cur || s.node.file.size > cur.node.file.size) {
          byLang.set(s.languageCode, s);
        }
      }
      const subtitles = [...byLang.values()];
      const kept = new Set(subtitles.map((s) => s.node.id));
      for (const s of allSubtitles) {
        if (!kept.has(s.node.id)) droppedSubtitleIds.add(s.node.id);
      }

      const multiple = subtitles.length > 1;
      subtitles.forEach((sub) => {
        canonicalNameById.set(
          sub.node.id,
          multiple
            ? `${base}.${sub.languageCode}${extOf(sub.node.name)}`
            : `${base}${extOf(sub.node.name)}`,
        );
      });

      return {
        seasonNumber,
        episodeNumber,
        video,
        subtitles,
        source: r.source,
        conflict: isConflict(r),
      };
    });

    return {
      seasonNumber,
      folderName: folder.name,
      folderId: folder.id,
      source: seasonSource,
      conflict: false, // set below
      episodes,
    };
  });

  seasons.sort((a, b) => a.seasonNumber - b.seasonNumber);
  const seasonConflict = markConflicts(seasons, (s) => s.seasonNumber);
  seasons.forEach((s) => (s.conflict = seasonConflict(s)));

  return {
    seasons,
    looseFiles,
    canonicalNameById,
    seasonLabelByFolderId,
    droppedSubtitleIds,
  };
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
