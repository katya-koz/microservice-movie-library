// "use client";

// import { useSearchParams } from "next/navigation";
// import Poster from "@/components/Poster";
// import PaginatedMediaPage from "@/components/media/PaginatedMediaPage";
// import { useWatchlist } from "@/hooks/useWatchlist";
// import { useMediaSummaries } from "@/hooks/useCatalogSummaries";
// import { useUser } from "@/context/UserContext";
// const PAGE_SIZE = 24;

// export default function MyListPage() {
//   const searchParams = useSearchParams();
//   const { user } = useUser();
//   const page = Number(searchParams.get("page")) || 1;

//   const {
//     data: watchlist,
//     isLoading: watchlistLoading,
//     error: watchlistError,
//   } = useWatchlist(user?.id);

//   const {
//     data: media = [],
//     isLoading: mediaLoading,
//     error: mediaError,
//   } = useMediaSummaries(watchlist ?? []);

//   const loading = watchlistLoading || mediaLoading;
//   const error = watchlistError || mediaError;

//   const totalElements = media.length;
//   const totalPages = Math.ceil(totalElements / PAGE_SIZE);

//   const start = (page - 1) * PAGE_SIZE;
//   const pageMedia = media.slice(start, start + PAGE_SIZE);

//   return (
//     <PaginatedMediaPage
//       title="My List"
//       media={pageMedia}
//       totalElements={totalElements}
//       totalPages={totalPages}
//       currentPage={page}
//       loading={loading}
//       error={error}
//       renderItem={(item) => (
//         <Poster
//           key={item.id}
//           summary={{
//             id: item.id,
//             mediaType: item.mediaType,
//             title: item.title,
//             posterPath: item.posterPath,
//           }}
//         ></Poster>
//       )}
//     />
//   );
// }
