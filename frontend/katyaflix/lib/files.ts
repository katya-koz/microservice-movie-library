export interface DroppedFile {
  file: File;
  /** Path relative to whatever the user dropped/selected, e.g. "Season 1/ep01.mkv" */
  relativePath: string;
}

function readAllDirectoryEntries(
  reader: FileSystemDirectoryReader,
): Promise<FileSystemEntry[]> {
  return new Promise((resolve, reject) => {
    let all: FileSystemEntry[] = [];
    const readBatch = () => {
      reader.readEntries((batch) => {
        if (!batch.length) {
          resolve(all);
          return;
        }
        // Chrome caps readEntries() at ~100 results per call, so we keep calling
        // until it returns empty.
        all = all.concat(batch);
        readBatch();
      }, reject);
    };
    readBatch();
  });
}

async function walkEntry(
  entry: FileSystemEntry,
  path = "",
): Promise<DroppedFile[]> {
  if (entry.isFile) {
    const file = await new Promise<File>((resolve, reject) =>
      (entry as FileSystemFileEntry).file(resolve, reject),
    );
    return [{ file, relativePath: path + entry.name }];
  }
  if (entry.isDirectory) {
    const reader = (entry as FileSystemDirectoryEntry).createReader();
    const entries = await readAllDirectoryEntries(reader);
    const nested = await Promise.all(
      entries.map((e) => walkEntry(e, `${path}${entry.name}/`)),
    );
    return nested.flat();
  }
  return [];
}

/** Handles a drop event's DataTransfer, expanding any dropped folders recursively. */
export async function collectFromDataTransfer(
  dataTransfer: DataTransfer,
): Promise<DroppedFile[]> {
  const items = dataTransfer.items;
  if (items && items.length > 0 && "webkitGetAsEntry" in items[0]) {
    const entries = Array.from(items)
      .map((item) => item.webkitGetAsEntry())
      .filter((e): e is FileSystemEntry => e !== null);
    if (entries.length > 0) {
      const nested = await Promise.all(
        entries.map((entry) => walkEntry(entry)),
      );
      return nested.flat();
    }
  }
  return Array.from(dataTransfer.files).map((file) => ({
    file,
    relativePath: file.name,
  }));
}

/** Handles a native <input type="file"> change (works for both file and directory pickers). */
export function collectFromFileList(fileList: FileList): DroppedFile[] {
  return Array.from(fileList).map((file) => ({
    file,
    relativePath:
      (file as File & { webkitRelativePath?: string }).webkitRelativePath ||
      file.name,
  }));
}
