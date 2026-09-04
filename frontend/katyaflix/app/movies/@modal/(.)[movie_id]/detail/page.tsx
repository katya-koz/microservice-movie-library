import MovieDetail from "@/components/movies/MovieDetail";
import Modal from "@/components/Modal";

export default async function Page({
  params,
}: {
  params: Promise<{ movie_id: string }>;
}) {
  const { movie_id } = await params;

  return (
    <Modal>
      <MovieDetail movieId={movie_id} />
    </Modal>
  );
}
