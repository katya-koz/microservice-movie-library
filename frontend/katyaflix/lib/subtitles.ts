/**
 * The HTML <track> element only renders WebVTT natively. SRT is the more
 * common format subtitle sites distribute, so we convert it losslessly
 * (timestamp comma → dot, strip cue numbers) rather than asking the user
 * to do it themselves.
 */
export function srtToVtt(srtText: string): string {
  const body = srtText
    .replace(/\r+/g, "")
    .trim()
    .replace(/^\d+\n(?=\d{2}:\d{2}:\d{2})/gm, "")
    .replace(/(\d{2}:\d{2}:\d{2}),(\d{3})/g, "$1.$2");
  return `WEBVTT\n\n${body}\n`;
}

export type SubtitleExt = "vtt" | "srt" | "ass" | "sub" | "other";

export function subtitleExt(fileName: string): SubtitleExt {
  const ext = fileName.split(".").pop()?.toLowerCase();
  if (ext === "vtt") return "vtt";
  if (ext === "srt") return "srt";
  if (ext === "ass" || ext === "ssa") return "ass";
  if (ext === "sub") return "sub";
  return "other";
}

/**
 * Builds a blob URL suitable for a <track src>. Returns supported: false
 * for formats we can't render natively (the file is still kept, just not
 * wired up as a caption track).
 */
export async function toTrackUrl(
  file: File,
): Promise<{ url: string; supported: boolean }> {
  const ext = subtitleExt(file.name);
  if (ext === "vtt") {
    return { url: URL.createObjectURL(file), supported: true };
  }
  if (ext === "srt") {
    const text = await file.text();
    const vtt = srtToVtt(text);
    const blob = new Blob([vtt], { type: "text/vtt" });
    return { url: URL.createObjectURL(blob), supported: true };
  }
  return { url: URL.createObjectURL(file), supported: false };
}
