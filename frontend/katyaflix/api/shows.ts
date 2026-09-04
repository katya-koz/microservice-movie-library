import { ShowPage } from "@/types/show";

export async function getShows(
  page = 0,
  size = 24,
  sort = "title,asc",
): Promise<ShowPage> {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
    sort,
  });

  const response = await fetch(
    `${process.env.NEXT_PUBLIC_API_ROOT}/shows?${params}`,
  );

  if (!response.ok) {
    throw new Error("Failed to fetch shows");
  }

  return response.json();
}

import { ShowDetail } from "@/types/show";

export async function getShowDetail(id: string): Promise<ShowDetail> {
  const response = await fetch(
    `${process.env.NEXT_PUBLIC_API_ROOT}/shows/${encodeURIComponent(id)}`,
  );
  if (!response.ok) {
    throw new Error(`Failed to fetch show: ${response.status}`);
  }

  return response.json();
}
