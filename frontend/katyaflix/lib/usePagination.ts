// import { useMemo, useState } from 'react';

// export function usePagination<T>(items: T[], pageSize: number) {
//   const [page, setPage] = useState(1);
//   const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
//   const clampedPage = Math.min(page, totalPages);

//   const pageItems = useMemo(() => {
//     const start = (clampedPage - 1) * pageSize;
//     return items.slice(start, start + pageSize);
//   }, [items, clampedPage, pageSize]);

//   return { page: clampedPage, totalPages, pageItems, setPage };
// }
