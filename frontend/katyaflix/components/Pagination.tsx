// interface PaginationProps {
//   page: number;
//   totalPages: number;
//   onPageChange: (page: number) => void;
// }

// export default function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
//   if (totalPages <= 1) return null;

//   const pages = getPageList(page, totalPages);

//   return (
//     <nav className="flex items-center justify-center gap-1" aria-label="Pagination">
//       <button
//         type="button"
//         onClick={() => onPageChange(page - 1)}
//         disabled={page === 1}
//         className="rounded px-3 py-2 font-mono text-xs uppercase tracking-widest text-paper-muted transition-colors hover:bg-ink-raised hover:text-paper disabled:pointer-events-none disabled:opacity-30"
//       >
//         Prev
//       </button>

//       {pages.map((p, i) =>
//         p === 'ellipsis' ? (
//           <span key={`e-${i}`} className="px-2 font-mono text-xs text-paper-faint">
//             …
//           </span>
//         ) : (
//           <button
//             key={p}
//             type="button"
//             onClick={() => onPageChange(p)}
//             aria-current={p === page ? 'page' : undefined}
//             className={`h-9 w-9 rounded font-mono text-xs transition-colors ${
//               p === page
//                 ? 'bg-marquee text-ink font-semibold'
//                 : 'text-paper-muted hover:bg-ink-raised hover:text-paper'
//             }`}
//           >
//             {p}
//           </button>
//         )
//       )}

//       <button
//         type="button"
//         onClick={() => onPageChange(page + 1)}
//         disabled={page === totalPages}
//         className="rounded px-3 py-2 font-mono text-xs uppercase tracking-widest text-paper-muted transition-colors hover:bg-ink-raised hover:text-paper disabled:pointer-events-none disabled:opacity-30"
//       >
//         Next
//       </button>
//     </nav>
//   );
// }

// function getPageList(page: number, totalPages: number): (number | 'ellipsis')[] {
//   const delta = 1;
//   const range: (number | 'ellipsis')[] = [];
//   const rangeStart = Math.max(2, page - delta);
//   const rangeEnd = Math.min(totalPages - 1, page + delta);

//   range.push(1);
//   if (rangeStart > 2) range.push('ellipsis');
//   for (let i = rangeStart; i <= rangeEnd; i++) range.push(i);
//   if (rangeEnd < totalPages - 1) range.push('ellipsis');
//   if (totalPages > 1) range.push(totalPages);

//   return range;
// }
