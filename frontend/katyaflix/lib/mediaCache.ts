// /**
//  * Object URLs only live as long as the tab/session does — they can't be
//  * serialized into localStorage the way catalog metadata can. This module
//  * keeps them in memory for the current session so playback works right
//  * after upload. After a full page reload the catalog entry still exists
//  * (metadata is persisted via the library store) but the video itself will
//  * need to be re-attached — see VideoPlayer's empty state.
//  *
//  * In a production build, replace this whole module with real uploads to
//  * your storage/CDN and store permanent URLs on the Movie/Episode records
//  * instead.
//  */
// export interface SubtitleUrl {
//   id: string;
//   url: string;
// }

// export interface MediaEntry {
//   videoUrl: string;
//   subtitles: SubtitleUrl[];
// }

// const cache = new Map<string, MediaEntry>();

// export function setMedia(key: string, entry: MediaEntry) {
//   cache.set(key, entry);
// }

// export function getMedia(key: string): MediaEntry | undefined {
//   return cache.get(key);
// }

// export function movieKey(movieId: string) {
//   return `movie:${movieId}`;
// }

// export function episodeKey(showId: string, episodeId: string) {
//   return `episode:${showId}:${episodeId}`;
// }
