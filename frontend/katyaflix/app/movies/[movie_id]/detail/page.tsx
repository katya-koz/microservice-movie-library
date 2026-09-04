import MovieDetail from "@/components/movies/MovieDetail";

export default async function MovieDetailModalPage({
  params,
}: {
  params: Promise<{ movie_id: string }>;
}) {
  const { movie_id } = await params;

  return <MovieDetail movieId={movie_id} />;
}
