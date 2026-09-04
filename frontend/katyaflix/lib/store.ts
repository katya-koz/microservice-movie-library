// import { create } from 'zustand';
// import { persist } from 'zustand/middleware';
// import { Movie, Show } from './types';

// interface LibraryState {
//   movies: Movie[];
//   shows: Show[];
//   addMovie: (movie: Movie) => void;
//   addShow: (show: Show) => void;
//   removeMovie: (id: string) => void;
//   removeShow: (id: string) => void;
// }

// export const useLibraryStore = create<LibraryState>()(
//   persist(
//     (set) => ({
//       movies: [],
//       shows: [],
//       addMovie: (movie) => set((state) => ({ movies: [movie, ...state.movies] })),
//       addShow: (show) => set((state) => ({ shows: [show, ...state.shows] })),
//       removeMovie: (id) => set((state) => ({ movies: state.movies.filter((m) => m.id !== id) })),
//       removeShow: (id) => set((state) => ({ shows: state.shows.filter((s) => s.id !== id) })),
//     }),
//     {
//       name: 'stacks-library',
//       // Only metadata is persisted — video/subtitle blob URLs live in
//       // lib/mediaCache.ts for the current session only. See that file.
//     }
//   )
// );
