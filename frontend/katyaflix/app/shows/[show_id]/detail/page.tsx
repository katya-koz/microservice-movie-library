import ShowDetail from "@/components/shows/ShowDetail";
import Modal from "@/components/Modal";

export default async function Page({
  params,
}: {
  params: Promise<{ show_id: string }>;
}) {
  const { show_id } = await params;

  return (
    <Modal>
      <ShowDetail showId={show_id} />
    </Modal>
  );
}
