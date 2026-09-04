// import { useEffect, useState } from 'react';

// /**
//  * The library store is persisted to localStorage, which doesn't exist on
//  * the server. Gate any store-dependent render behind this so the first
//  * client paint doesn't mismatch the server-rendered (empty) HTML.
//  */
// export function useHydrated(): boolean {
//   const [hydrated, setHydrated] = useState(false);
//   useEffect(() => setHydrated(true), []);
//   return hydrated;
// }
