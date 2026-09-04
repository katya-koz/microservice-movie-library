// 'use client';

// import { useEffect } from 'react';
// import { CloseIcon } from './icons';
// import { SubtitleTrack } from '@/lib/types';
// import { SubtitleUrl } from '@/lib/mediaCache';

// interface PlayerModalProps {
//   title: string;
//   subtitle?: string;
//   videoUrl?: string;
//   subtitles: SubtitleTrack[];
//   subtitleUrls: SubtitleUrl[];
//   onClose: () => void;
// }

// export default function PlayerModal({ title, subtitle, videoUrl, subtitles, subtitleUrls, onClose }: PlayerModalProps) {
//   useEffect(() => {
//     function onKeyDown(e: KeyboardEvent) {
//       if (e.key === 'Escape') onClose();
//     }
//     document.addEventListener('keydown', onKeyDown);
//     document.body.style.overflow = 'hidden';
//     return () => {
//       document.removeEventListener('keydown', onKeyDown);
//       document.body.style.overflow = '';
//     };
//   }, [onClose]);

//   const urlById = new Map(subtitleUrls.map((s) => [s.id, s.url]));

//   return (
//     <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 p-4 animate-fade-in" onClick={onClose}>
//       <div
//         className="w-full max-w-4xl overflow-hidden rounded-lg border border-ink-line bg-ink shadow-modal animate-rise-in"
//         onClick={(e) => e.stopPropagation()}
//       >
//         <div className="flex items-center justify-between border-b border-ink-line px-4 py-3">
//           <div className="min-w-0">
//             <p className="truncate font-display text-lg font-bold text-paper">{title}</p>
//             {subtitle && <p className="truncate font-mono text-xs text-paper-muted">{subtitle}</p>}
//           </div>
//           <button
//             type="button"
//             onClick={onClose}
//             aria-label="Close player"
//             className="flex-shrink-0 rounded p-1.5 text-paper-muted hover:bg-ink-raised hover:text-paper"
//           >
//             <CloseIcon className="h-5 w-5" />
//           </button>
//         </div>

//         {videoUrl ? (
//           <video controls autoPlay className="aspect-video w-full bg-black" crossOrigin="anonymous">
//             <source src={videoUrl} />
//             {subtitles
//               .filter((s) => s.supported && urlById.has(s.id))
//               .map((s, i) => (
//                 <track key={s.id} kind="subtitles" label={s.label} src={urlById.get(s.id)} default={i === 0} />
//               ))}
//             Your browser doesn&rsquo;t support embedded video.
//           </video>
//         ) : (
//           <div className="flex aspect-video flex-col items-center justify-center gap-2 bg-ink-raised px-8 text-center">
//             <p className="font-medium text-paper">This video isn&rsquo;t available in the current session.</p>
//             <p className="max-w-sm font-mono text-xs text-paper-muted">
//               Video files live in memory for the session that uploaded them. Re-upload the file to play it again,
//               or wire this starter up to real storage.
//             </p>
//           </div>
//         )}
//       </div>
//     </div>
//   );
// }
